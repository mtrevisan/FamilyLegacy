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
package io.github.mtrevisan.familylegacy.imaging;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;


/**
 * Finds the faces in a picture (the SPI of the face detectors).
 * <p>
 * The detector is a plugin, as the importers and the report formats are: a jar {@code face-detector.jar} in the {@code plugins}
 * directory, found as a {@link java.util.ServiceLoader} provider (see {@link FaceDetectors}). Without it nothing is proposed and the crop of a
 * portrait is only drawn by hand.
 * </p>
 */
public interface FaceDetector{

	/** @return	What identifies the detector. */
	String id();

	/**
	 * Finds the faces. It can take long: call it away from the event dispatch thread.
	 *
	 * @param image	The picture.
	 * @return	The box of each face, in the pixels of the picture (the face only, from the forehead to the chin: see
	 * 	{@link PortraitCrops}), the most certain first; empty if there is none.
	 * @throws IOException	If the detector cannot work (a missing library, an unreadable model).
	 */
	List<Rectangle> detectFaces(BufferedImage image) throws IOException;

	/**
	 * Prepares the detector (native libraries, model) so that the first real detection is immediate. Call it away from the event dispatch
	 * thread; the default does nothing.
	 *
	 * @throws IOException	If the detector cannot work.
	 */
	default void warmUp() throws IOException{}

}
