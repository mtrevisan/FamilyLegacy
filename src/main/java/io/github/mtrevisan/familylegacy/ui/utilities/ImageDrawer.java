/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.utilities;

import java.awt.*;


/**
 * This utility class draws and scales an image to fit canvas of a component.
 * <p>If the image is smaller than the canvas, it is kept as it is.</p>
 */
public class ImageDrawer{

	public static final int ALIGNMENT_X_LEFT = -1;
	public static final int ALIGNMENT_X_CENTER = 0;
	public static final int ALIGNMENT_X_RIGHT = 1;

	public static final int ALIGNMENT_Y_TOP = -1;
	public static final int ALIGNMENT_Y_MIDDLE = 0;
	public static final int ALIGNMENT_Y_BOTTOM = 1;


	public static void drawScaledImage(final Image image, final Component parent, final Graphics g,
			final int alignmentX, final int alignmentY){
		final int imageWidth = image.getWidth(null);
		final int imageHeight = image.getHeight(null);
		final float imageAspect = (float)imageHeight / imageWidth;

		int parentWidth = parent.getWidth();
		int parentHeight = parent.getHeight();
		final float parentAspect = (float)parentHeight / parentWidth;

		//top left X position
		int x1 = 0;
		//top left Y position
		int y1 = 0;
		//bottom right X position
		int x2;
		//bottom right Y position
		int y2;
		if(imageWidth < parentWidth && imageHeight < parentHeight){
			//the image is smaller than the canvas:
			if(alignmentX == ALIGNMENT_X_CENTER)
				x1 = (parentWidth - imageWidth) / 2;
			else if(alignmentX == ALIGNMENT_X_RIGHT)
				x1 = parentWidth - imageWidth;
			if(alignmentY == ALIGNMENT_Y_MIDDLE)
				y1 = (parentHeight - imageHeight) / 2;
			else if(alignmentY == ALIGNMENT_Y_BOTTOM)
				y1 = parentHeight - imageHeight;
			x2 = x1 + imageWidth;
			y2 = y1 + imageHeight;
		}
		else{
			if(parentAspect > imageAspect){
				y1 = parentHeight;
				//keep image aspect ratio
				parentHeight = (int)(parentWidth * imageAspect);
				if(alignmentY == ALIGNMENT_Y_MIDDLE)
					y1 = (y1 - parentHeight) / 2;
				else if(alignmentY == ALIGNMENT_Y_BOTTOM)
					y1 = y1 - parentHeight;
			}
			else{
				x1 = parentWidth;
				//keep image aspect ratio
				parentWidth = (int)(parentHeight / imageAspect);
				if(alignmentX == ALIGNMENT_X_CENTER)
					x1 = (x1 - parentWidth) / 2;
				else if(alignmentX == ALIGNMENT_X_RIGHT)
					x1 = x1 - parentWidth;
			}

			x2 = x1 + parentWidth;
			y2 = y1 + parentHeight;
		}

		g.drawImage(image, x1, y1, x2, y2, 0, 0, imageWidth, imageHeight, null);
	}

}
