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
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;

import java.awt.Rectangle;


/**
 * Handler for CROP structure.
 * <p>
 * Structure:
 * <pre>
 * // Cropping rectangle expressed in image pixels.
 * struct CropRect {
 *   x: Int
 *   y: Int
 *   width: Int
 *   height: Int
 *
 *   require width > 0
 *   require height > 0
 * }
 * </pre>
 */
public final class CropReader{

	public static final String TAG_X = "x";
	public static final String TAG_Y = "y";
	public static final String TAG_WIDTH = "width";
	public static final String TAG_HEIGHT = "height";


	private CropReader(){}


	public static Rectangle extractPreferredImageCrop(final FLEFRecord crop){
		if(crop != null){
			final String x = FLEFRecordHelper.getChildValue(crop, TAG_X);
			final String y = FLEFRecordHelper.getChildValue(crop, TAG_Y);
			final String width = FLEFRecordHelper.getChildValue(crop, TAG_WIDTH);
			final String height = FLEFRecordHelper.getChildValue(crop, TAG_HEIGHT);
			if(x != null && y != null && width != null && height != null){
				try{
					final int cropX = Integer.parseInt(x);
					final int cropY = Integer.parseInt(y);
					final int cropWidth = Integer.parseInt(width);
					final int cropHeight = Integer.parseInt(height);
					if(cropX >= 0 && cropY >= 0 && cropWidth >= 0 && cropHeight >= 0)
						return new Rectangle(cropX, cropY, cropWidth, cropHeight);
				}
				catch(final Exception ignored){}
			}
		}
		return null;
	}

//	public static String extractUri(final FLEFRecord record){
//		return FLEFRecordHelper.getChildValue(record, TAG_URI);
//	}

}
