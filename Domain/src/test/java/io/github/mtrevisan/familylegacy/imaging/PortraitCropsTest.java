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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.util.List;


/** The crop of the whole head, from the box of the face. */
class PortraitCropsTest{

	@Test
	void headIsWiderThanTheFaceAndInThePortraitProportions(){
		final Rectangle crop = PortraitCrops.headCrop(new Rectangle(400, 300, 100, 125), 1000, 1000);

		Assertions.assertEquals(180, crop.width);
		Assertions.assertEquals(240, crop.height);
		//the face is inside, about at the center
		Assertions.assertTrue(crop.contains(new Rectangle(400, 300, 100, 125)));
		Assertions.assertEquals(450., crop.getCenterX(), 1.);
	}

	@Test
	void cropStaysInsideThePictureKeepingTheProportions(){
		//a face at the corner of the picture
		final Rectangle corner = PortraitCrops.headCrop(new Rectangle(0, 0, 100, 125), 1000, 1000);
		Assertions.assertEquals(new Rectangle(0, 0, 180, 240), corner);

		//a face too big for the picture: the crop is made smaller, 3:4 all the same
		final Rectangle big = PortraitCrops.headCrop(new Rectangle(10, 10, 300, 300), 400, 400);
		Assertions.assertTrue(new Rectangle(0, 0, 400, 400).contains(big));
		Assertions.assertEquals(big.width * 4 / 3., big.height, 1.);
	}

	@Test
	void emptyFaceGivesNoCrop(){
		Assertions.assertNull(PortraitCrops.headCrop(new Rectangle(10, 10, 0, 10), 100, 100));
		Assertions.assertNull(PortraitCrops.headCrop(null, 100, 100));
		Assertions.assertNull(PortraitCrops.headCrop(new Rectangle(1, 1, 2, 2), 100, 100));
	}

	@Test
	void clickPicksTheNearestFaceAmongTheCropsThatContainIt(){
		final List<PortraitCrops.Proposal> proposals = PortraitCrops.proposals(
			List.of(new Rectangle(100, 100, 100, 125), new Rectangle(220, 100, 100, 125)), 1000, 1000);
		Assertions.assertEquals(2, proposals.size());

		//(the two heads overlap between x=190 and x=230)
		Assertions.assertSame(proposals.get(0), PortraitCrops.pick(proposals, 195, 160));
		Assertions.assertSame(proposals.get(1), PortraitCrops.pick(proposals, 225, 160));
		Assertions.assertSame(proposals.get(1), PortraitCrops.pick(proposals, 300, 160));
		//outside every crop: nothing is proposed
		Assertions.assertNull(PortraitCrops.pick(proposals, 700, 700));
	}

}
