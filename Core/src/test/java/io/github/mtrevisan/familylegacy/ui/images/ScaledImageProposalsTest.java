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

import io.github.mtrevisan.familylegacy.imaging.PortraitCrops;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;


/** The click on a face found makes its head the crop; a click elsewhere leaves everything as it is. */
class ScaledImageProposalsTest{

	private ScaledImage holder;
	private PortraitCrops.Proposal proposal;


	@BeforeEach
	void setUp(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		holder = ScaledImage.create();
		holder.setRectangularImage(new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB));
		holder.setSize(400, 300);
		//(the first paint fits the image in the component: scale 1 here, and no offset)
		final BufferedImage canvas = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
		holder.paint(canvas.getGraphics());

		proposal = PortraitCrops.proposals(List.of(new Rectangle(150, 80, 50, 62)), 400, 300).get(0);
		holder.setCropProposals(List.of(proposal));
	}

	@Test
	void clickInsideTheProposalSetsTheCrop(){
		click(170, 110);

		Assertions.assertEquals(proposal.crop(), holder.getCrop());
	}

	@Test
	void clickOutsideEveryProposalDoesNothing(){
		click(20, 20);

		Assertions.assertNull(holder.getCrop());
	}

	@Test
	void dragStillDrawsByHand(){
		press(10, 10);
		drag(60, 70);
		release(60, 70);

		Assertions.assertEquals(new Rectangle(10, 10, 50, 60), holder.getCrop());
	}

	@Test
	void withoutProposalsAClickDoesNothing(){
		holder.setCropProposals(null);

		click(170, 110);

		Assertions.assertNull(holder.getCrop());
	}


	private void click(final int x, final int y){
		press(x, y);
		release(x, y);
	}

	private void press(final int x, final int y){
		holder.dispatchEvent(event(MouseEvent.MOUSE_PRESSED, x, y));
	}

	private void drag(final int x, final int y){
		holder.dispatchEvent(event(MouseEvent.MOUSE_DRAGGED, x, y));
	}

	private void release(final int x, final int y){
		holder.dispatchEvent(event(MouseEvent.MOUSE_RELEASED, x, y));
	}

	private MouseEvent event(final int id, final int x, final int y){
		return new MouseEvent(holder, id, System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1);
	}

}
