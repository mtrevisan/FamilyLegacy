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
import java.util.ArrayList;
import java.util.List;


/**
 * From the boxes of the faces to the crops of the portrait: the whole head, in the proportions of the portrait boxes, inside the picture.
 */
public final class PortraitCrops{

	/** Height of the portrait boxes over their width. */
	public static final double PORTRAIT_HEIGHT_OVER_WIDTH = 4. / 3.;

	/** Width of the head (with hair and ears) over the width of the box of the face (forehead to chin). */
	private static final double HEAD_WIDTH_OVER_FACE_WIDTH = 1.4;
	/** How much of the height of the face the head is moved up: the hair is over the forehead, the box of the face ends at the chin. */
	private static final double HEAD_RISE_OVER_FACE_HEIGHT = 0.1;
	private static final int MIN_SIZE = 8;


	/**
	 * A face and the crop of the head that was computed from it.
	 *
	 * @param face	The box of the face.
	 * @param crop	The crop of the whole head.
	 */
	public record Proposal(Rectangle face, Rectangle crop){}


	private PortraitCrops(){}


	/**
	 * @param faces	The boxes of the faces.
	 * @param imageWidth	The width of the picture.
	 * @param imageHeight	The height of the picture.
	 * @return	The proposals, in the order of the faces; a face that gives no crop is left out.
	 */
	public static List<Proposal> proposals(final List<Rectangle> faces, final int imageWidth, final int imageHeight){
		final List<Proposal> proposals = new ArrayList<>(faces.size());
		for(final Rectangle face : faces){
			final Rectangle crop = headCrop(face, imageWidth, imageHeight);
			if(crop != null)
				proposals.add(new Proposal(new Rectangle(face), crop));
		}
		return proposals;
	}

	/**
	 * The whole head: wider than the face, in the proportions of the portrait, centered on the face a little raised; if it does not fit in
	 * the picture it is made smaller (the proportions stay), and moved inside.
	 *
	 * @param face	The box of the face.
	 * @param imageWidth	The width of the picture.
	 * @param imageHeight	The height of the picture.
	 * @return	The crop, or {@code null} if the face or the picture are empty.
	 */
	public static Rectangle headCrop(final Rectangle face, final int imageWidth, final int imageHeight){
		if(face == null || face.width <= 0 || face.height <= 0 || imageWidth <= 0 || imageHeight <= 0)
			return null;

		double width = face.width * HEAD_WIDTH_OVER_FACE_WIDTH;
		double height = width * PORTRAIT_HEIGHT_OVER_WIDTH;
		if(width > imageWidth){
			width = imageWidth;
			height = width * PORTRAIT_HEIGHT_OVER_WIDTH;
		}
		if(height > imageHeight){
			height = imageHeight;
			width = height / PORTRAIT_HEIGHT_OVER_WIDTH;
		}

		final double centerX = face.x + face.width / 2.;
		final double centerY = face.y + face.height / 2. - face.height * HEAD_RISE_OVER_FACE_HEIGHT;
		final int cropWidth = Math.max(1, (int)Math.round(width));
		final int cropHeight = Math.max(1, (int)Math.round(height));
		final int x = Math.clamp((int)Math.round(centerX - cropWidth / 2.), 0, imageWidth - cropWidth);
		final int y = Math.clamp((int)Math.round(centerY - cropHeight / 2.), 0, imageHeight - cropHeight);
		return (cropWidth >= MIN_SIZE && cropHeight >= MIN_SIZE? new Rectangle(x, y, cropWidth, cropHeight): null);
	}

	/**
	 * Chooses the proposal for a click: among the crops that contain the point, the one whose face is nearest to it (heads of a group
	 * overlap).
	 *
	 * @param proposals	The proposals.
	 * @param x	The abscissa of the click, in the pixels of the picture.
	 * @param y	The ordinate of the click, in the pixels of the picture.
	 * @return	The proposal, or {@code null} if no crop contains the point.
	 */
	public static Proposal pick(final List<Proposal> proposals, final int x, final int y){
		Proposal best = null;
		double bestDistance = Double.MAX_VALUE;
		for(final Proposal proposal : proposals){
			if(!proposal.crop().contains(x, y))
				continue;

			final double dx = x - proposal.face().getCenterX();
			final double dy = y - proposal.face().getCenterY();
			final double distance = dx * dx + dy * dy;
			if(distance < bestDistance){
				bestDistance = distance;
				best = proposal;
			}
		}
		return best;
	}

}
