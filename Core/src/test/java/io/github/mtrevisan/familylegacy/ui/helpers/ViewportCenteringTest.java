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
package io.github.mtrevisan.familylegacy.ui.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;


/** A view starts with the component in the middle of the window, when the content is larger than the window. */
class ViewportCenteringTest{

	/** A content of 2000 x 1000 with a 200 x 100 component at (900, 400), in a window of 400 x 300. */
	private static final class Fixture{

		final JPanel content = new JPanel(null);
		final JPanel target = new JPanel();
		final JScrollPane scroll;


		Fixture(final int contentWidth, final int contentHeight, final int windowWidth, final int windowHeight){
			content.setPreferredSize(new Dimension(contentWidth, contentHeight));
			target.setBounds(900, 400, 200, 100);
			content.add(target);
			scroll = new JScrollPane(content);
			scroll.setBounds(0, 0, windowWidth, windowHeight);
			scroll.doLayout();
			scroll.getViewport().doLayout();
		}

	}


	@Test
	void theComponentGoesToTheMiddleOfTheWindowInBothDirections(){
		final Fixture fixture = new Fixture(2000, 1000, 400, 300);

		Assertions.assertTrue(ViewportCentering.centerOn(fixture.scroll, fixture.target, true, true));

		//the middle of the component is (1000, 450): the window of 400 x 300 starts at (800, 300), the scroll bars take their room
		final Point position = fixture.scroll.getViewport().getViewPosition();
		final Dimension extent = fixture.scroll.getViewport().getExtentSize();
		Assertions.assertEquals(1000, position.x + extent.width / 2);
		Assertions.assertEquals(450, position.y + extent.height / 2);
	}

	@Test
	void onlyTheDirectionsAskedForAreMoved(){
		final Fixture fixture = new Fixture(2000, 1000, 400, 300);
		fixture.scroll.getViewport().setViewPosition(new Point(0, 50));

		ViewportCentering.centerOn(fixture.scroll, fixture.target, true, false);

		final Point position = fixture.scroll.getViewport().getViewPosition();
		Assertions.assertTrue(position.x > 0, "centered horizontally");
		Assertions.assertEquals(50, position.y, "the vertical position is left as it was");
	}

	@Test
	void aComponentNearTheEdgeDoesNotScrollOutOfTheContent(){
		final Fixture fixture = new Fixture(2000, 1000, 400, 300);
		fixture.target.setBounds(0, 0, 100, 100);

		ViewportCentering.centerOn(fixture.scroll, fixture.target, true, true);

		Assertions.assertEquals(new Point(0, 0), fixture.scroll.getViewport().getViewPosition());
	}

	@Test
	void aContentThatFitsTheWindowStaysWhereItIs(){
		final Fixture fixture = new Fixture(300, 200, 400, 300);

		ViewportCentering.centerOn(fixture.scroll, fixture.content, true, true);

		Assertions.assertEquals(new Point(0, 0), fixture.scroll.getViewport().getViewPosition(), "the layout centers it");
	}

	@Test
	void theViewItselfCanBeTheTarget(){
		final Fixture fixture = new Fixture(2000, 1000, 400, 300);

		ViewportCentering.centerOn(fixture.scroll, fixture.content, true, false);

		final Point position = fixture.scroll.getViewport().getViewPosition();
		final Dimension extent = fixture.scroll.getViewport().getExtentSize();
		Assertions.assertEquals(1000, position.x + extent.width / 2);
	}

	@Test
	void aWindowWithoutASizeDoesNothingYet(){
		final JPanel content = new JPanel();
		content.setPreferredSize(new Dimension(2000, 1000));
		final JScrollPane scroll = new JScrollPane(content);

		Assertions.assertFalse(ViewportCentering.centerOn(scroll, content, true, true));
	}

	@Test
	void whenTheWindowGetsItsSizeLaterTheViewIsCentered() throws InvocationTargetException, InterruptedException{
		final Fixture fixture = new Fixture(2000, 1000, 0, 0);

		SwingUtilities.invokeAndWait(() -> ViewportCentering.centerWhenReady(fixture.scroll, () -> fixture.target, true, true));
		//nothing yet: the window has no size
		SwingUtilities.invokeAndWait(() -> {});
		Assertions.assertEquals(new Point(0, 0), fixture.scroll.getViewport().getViewPosition());

		SwingUtilities.invokeAndWait(() -> {
			fixture.scroll.setBounds(new Rectangle(0, 0, 400, 300));
			fixture.scroll.doLayout();
			fixture.scroll.getViewport().setBounds(0, 0, 400, 300);
			fixture.scroll.getViewport().doLayout();
		});
		//the centering is queued after the resize
		SwingUtilities.invokeAndWait(() -> {});
		SwingUtilities.invokeAndWait(() -> {});

		final Point position = fixture.scroll.getViewport().getViewPosition();
		Assertions.assertTrue(position.x > 0 && position.y > 0, position.toString());
	}

}
