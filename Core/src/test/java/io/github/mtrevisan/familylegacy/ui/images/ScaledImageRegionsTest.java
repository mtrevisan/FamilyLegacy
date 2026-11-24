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
package io.github.mtrevisan.familylegacy.ui.images;

import io.github.mtrevisan.familylegacy.imaging.MediaRegions.Region;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


/**
 * The regions over the image of the viewer: which ones are under the mouse (the smallest first), the tooltip, the click that is not a pan, and
 * how they are drawn.
 */
class ScaledImageRegionsTest{

	/** The whole image, a face in it, and another face that overlaps the first one. */
	private static final Region GROUP = new Region(new Rectangle(0, 0, 400, 300), null, List.of("I1"));
	private static final Region FACE = new Region(new Rectangle(160, 120, 40, 30), null, List.of("I2"));
	private static final Region OTHER_FACE = new Region(new Rectangle(180, 135, 40, 30), null, List.of("I3"));

	private ScaledImage holder;
	private final List<List<Region>> clicks = new ArrayList<>();
	private final List<Point> clickPoints = new ArrayList<>();


	@BeforeEach
	void setUp(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		holder = ScaledImage.createViewOnly();
		final BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
		holder.setRectangularImage(image);
		holder.setSize(400, 300);
		//(the first paint fits the image in the component: scale 1 here, and no offset)
		holder.paint(new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB).getGraphics());

		holder.setRegions(List.of(new RegionOverlay.Mark(GROUP, false), new RegionOverlay.Mark(FACE, false),
			new RegionOverlay.Mark(OTHER_FACE, false)), null);
		holder.setRegionClickListener((regions, point) -> {
			clicks.add(regions);
			clickPoints.add(point);
		});
		holder.setRegionTooltip(regions -> String.join(",", regions.stream().map(region -> region.individuals().getFirst()).toList()));
	}


	@Test
	void clickOnAFaceTellsTheSmallestRegionFirst(){
		click(170, 130);

		//(.425 and .433 of the image: only in the group and in the first face)
		Assertions.assertEquals(List.of(List.of(FACE, GROUP)), clicks);
		Assertions.assertEquals(new Point(170, 130), clickPoints.getFirst());
	}

	@Test
	void clickWhereTheFacesOverlapTellsThemAll(){
		click(190, 145);

		Assertions.assertEquals(List.of(List.of(FACE, OTHER_FACE, GROUP)), clicks);
	}

	@Test
	void clickOnTheGroupOnlyTellsTheGroup(){
		click(20, 20);

		Assertions.assertEquals(List.of(List.of(GROUP)), clicks);
	}

	@Test
	void clickOutsideEveryRegionDoesNothing(){
		holder.setRegions(List.of(new RegionOverlay.Mark(FACE, false)), null);

		click(20, 20);

		Assertions.assertTrue(clicks.isEmpty());
	}

	@Test
	void hiddenRegionsAreNotClicked(){
		holder.setRegionsVisible(false);

		click(170, 130);

		Assertions.assertTrue(clicks.isEmpty());
		Assertions.assertEquals("", tooltip(170, 130));

		holder.setRegionsVisible(true);
		click(170, 130);
		Assertions.assertEquals(1, clicks.size());
	}

	@Test
	void aDragIsNotAClick(){
		press(170, 130, 0);
		release(220, 180, 0);

		Assertions.assertTrue(clicks.isEmpty());
	}

	@Test
	void aClickWithControlIsAPanNotAClick(){
		press(170, 130, InputEvent.CTRL_DOWN_MASK);
		release(170, 130, InputEvent.CTRL_DOWN_MASK);

		Assertions.assertTrue(clicks.isEmpty());
	}

	@Test
	void theRightButtonIsNotAClick(){
		holder.dispatchEvent(new MouseEvent(holder, MouseEvent.MOUSE_PRESSED, 0L, MouseEvent.BUTTON3_DOWN_MASK, 170, 130, 1, false, MouseEvent.BUTTON3));
		holder.dispatchEvent(new MouseEvent(holder, MouseEvent.MOUSE_RELEASED, 0L, MouseEvent.BUTTON3_DOWN_MASK, 170, 130, 1, false, MouseEvent.BUTTON3));

		Assertions.assertTrue(clicks.isEmpty());
	}

	@Test
	void theTooltipListsTheRegionsUnderTheMouse(){
		Assertions.assertEquals("I2,I1", tooltip(170, 130));
		Assertions.assertEquals("I2,I3,I1", tooltip(190, 145));
		Assertions.assertEquals("I1", tooltip(20, 20));
	}

	@Test
	void anImageWithNoRegionsHasNoTooltipAndNoClick(){
		holder.setRegions(List.of(), null);

		click(170, 130);

		Assertions.assertTrue(clicks.isEmpty());
		Assertions.assertNull(holder.getToolTipText(event(MouseEvent.MOUSE_MOVED, 170, 130, 0)));
	}

	@Test
	void theRegionsOfAnotherPageAreNotThere(){
		final Region secondPage = new Region(new Rectangle(0, 0, 400, 300), 2, List.of("I9"));
		holder.setRegions(List.of(new RegionOverlay.Mark(FACE, false), new RegionOverlay.Mark(secondPage, false)), null);

		Assertions.assertEquals("I2", tooltip(170, 130));
	}


	@Test
	void theRegionsAreDrawnAndCanBeHidden(){
		holder.setRegions(List.of(new RegionOverlay.Mark(new Region(new Rectangle(100, 75, 200, 150), null, List.of()), false)), null);

		final BufferedImage shown = paint();
		//(the white line on the left side of the region: 25% of 400 pixels)
		Assertions.assertTrue(isBrightnessAbove(shown, 100, 150, 90), "the line is drawn");
		Assertions.assertFalse(isBrightnessAbove(shown, 50, 50, 90), "outside of the region the image is untouched");

		holder.setRegionsVisible(false);
		Assertions.assertFalse(isBrightnessAbove(paint(), 100, 150, 90), "hidden: nothing is drawn");
	}

	@Test
	void aPortraitHasABadgeInItsTopRightCornerWhenThereIsRoomForIt(){
		final Region large = new Region(new Rectangle(40, 30, 240, 180), null, List.of("I1"));
		holder.setRegions(List.of(new RegionOverlay.Mark(large, true)), null);

		//(the region is at 40..280 and 30..210: the badge is a blue circle of 18 pixels, 2 inside the corner)
		final BufferedImage withBadge = paint();
		final Color badge = new Color(withBadge.getRGB(280 - 2 - 9 - 4, 30 + 2 + 9 + 6));
		Assertions.assertTrue(badge.getBlue() > 150 && badge.getRed() < 60, "blue: " + badge);

		holder.setRegions(List.of(new RegionOverlay.Mark(large, false)), null);
		final Color none = new Color(paint().getRGB(280 - 2 - 9 - 4, 30 + 2 + 9 + 6));
		Assertions.assertFalse(none.getBlue() > 150 && none.getRed() < 60, "no badge for the others: " + none);
	}

	@Test
	void aSmallPortraitHasNoBadgeButAThickerLine(){
		final Region small = new Region(new Rectangle(100, 75, 16, 12), null, List.of("I1"));

		holder.setRegions(List.of(new RegionOverlay.Mark(small, true)), null);
		final BufferedImage portrait = paint();
		holder.setRegions(List.of(new RegionOverlay.Mark(small, false)), null);
		final BufferedImage plain = paint();

		//(16 x 12 pixels on the screen: under the size of the badge; the left side is at x = 100)
		int differences = 0;
		for(int x = 95; x < 106; x ++)
			if(portrait.getRGB(x, 80) != plain.getRGB(x, 80))
				differences ++;
		Assertions.assertTrue(differences > 0, "the line of a portrait is thicker");
		Assertions.assertFalse(isBlue(portrait, 100 + 16 - 2 - 9, 75 + 2 + 9), "and there is no badge");
	}


	private BufferedImage paint(){
		final BufferedImage canvas = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
		holder.paint(canvas.getGraphics());
		return canvas;
	}

	private static boolean isBrightnessAbove(final BufferedImage image, final int x, final int y, final int threshold){
		//(the sides of the line are antialiased: the pixels around are looked at)
		for(int dx = -1; dx <= 1; dx ++){
			final Color color = new Color(image.getRGB(x + dx, y));
			if(color.getRed() > threshold && color.getGreen() > threshold && color.getBlue() > threshold)
				return true;
		}
		return false;
	}

	private static boolean isBlue(final BufferedImage image, final int x, final int y){
		final Color color = new Color(image.getRGB(x, y));
		return (color.getBlue() > 150 && color.getRed() < 60);
	}

	private String tooltip(final int x, final int y){
		final String text = holder.getToolTipText(event(MouseEvent.MOUSE_MOVED, x, y, 0));
		return (text != null? text: "");
	}

	private void click(final int x, final int y){
		press(x, y, 0);
		release(x, y, 0);
	}

	private void press(final int x, final int y, final int modifiers){
		holder.dispatchEvent(event(MouseEvent.MOUSE_PRESSED, x, y, modifiers));
	}

	private void release(final int x, final int y, final int modifiers){
		holder.dispatchEvent(event(MouseEvent.MOUSE_RELEASED, x, y, modifiers));
	}

	private MouseEvent event(final int id, final int x, final int y, final int modifiers){
		return new MouseEvent(holder, id, System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK | modifiers, x, y, 1, false, MouseEvent.BUTTON1);
	}

}
