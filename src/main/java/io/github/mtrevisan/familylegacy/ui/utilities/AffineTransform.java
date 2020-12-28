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


public class AffineTransform{

	private double scale = 1.;
	private double translateX;
	private double translateY;


	public double getTranslateX(){
		return translateX;
	}

	public double getTranslateY(){
		return translateY;
	}

	public void setScale(final double scale){
		this.scale = scale;
	}

	public void addScale(final double ds){
		scale *= ds;
	}

	public boolean addZoom(final double zoomFactor, final double minZoom, final double maxZoom, final int zoomPointX, final int zoomPointY){
		final double newZoom = scale * zoomFactor;
		if(minZoom <= newZoom && newZoom <= maxZoom){
			final double k = (1. - zoomFactor) / newZoom;
			final double dx = (zoomPointX - translateX) * k;
			final double dy = (zoomPointY - translateY) * k;
			addScale(zoomFactor);
			addTranslation(dx, dy);

			return true;
		}
		return false;
	}

	public void setTranslation(final double translateX, final double translateY){
		this.translateX = translateX;
		this.translateY = translateY;
	}

	public void addTranslation(final double dx, final double dy){
		translateX += dx;
		translateY += dy;
	}

	public int transformX(final double x){
		return (int)(x * scale + translateX);
	}

	public int transformY(final double y){
		return (int)(y * scale + translateY);
	}

	public int transformInverseX(final double x){
		return (int)((x - translateX) / scale);
	}

	public int transformInverseY(final double y){
		return (int)((y - translateY) / scale);
	}

}
