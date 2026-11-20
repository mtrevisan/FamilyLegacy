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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.readers.CropReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportSection;

import java.awt.Rectangle;
import java.nio.file.Path;


/** The preferred image (the portrait) of a person, as a report embeds it: whole or cropped, reduced, with the name of the person as caption. */
final class Portraits{

	private static final String TAG_PREFERRED_DOCUMENT = FLEFRecordHelper.composePath(IndividualReader.TAG_PREFERRED_IMAGE, CropReader.TAG_MEDIA);


	private Portraits(){}


	/**
	 * @param ctx	The context of the report.
	 * @param person	The person.
	 * @param preferred	The {@code preferred_image} node of the person.
	 * @param uri	The file of the image.
	 * @param baseCaption	The caption if the options do not say another one.
	 * @return	The image to embed.
	 * @throws java.nio.file.InvalidPathException	If the URI is not a path.
	 */
	static ReportSection.Image image(final ReportContext ctx, final FLEFRecord person, final FLEFRecord preferred, final String uri,
			final String baseCaption){
		final ReportConfig config = ctx.config;
		final Rectangle crop = (config.portraitMode() != PortraitMode.WHOLE? IndividualReader.extractPreferredImageCrop(person): null);

		final String caption;
		if(config.portraitNameTitle())
			caption = ctx.narrativeName(person);
		else
			//(a crop that is the image needs no mention of its region)
			caption = (crop != null? baseCaption: ReportFormatters.imageCaption(preferred, baseCaption, ctx.labels));
		final Path file = ReportImages.prepare(Path.of(uri), crop, config.thumbnailSize(), config.maxImageMegapixels());
		return new ReportSection.Image(file, caption, config.portraitAlignment());
	}

	/**
	 * @param ctx	The context of the report.
	 * @param person	A person.
	 * @return	The identifier of the picture the portrait of the person is cut from, if the picture is left out of the media because only
	 * 	the portrait is wanted; {@code null} otherwise.
	 */
	static String omittedOriginalOf(final ReportContext ctx, final FLEFRecord person){
		if(ctx.config.portraitMode() != PortraitMode.CROP_WITHOUT_ORIGINAL || !ctx.config.includePictures()
				|| IndividualReader.extractPreferredImageCrop(person) == null)
			return null;

		return FLEFRecordHelper.getChildValue(person, TAG_PREFERRED_DOCUMENT);
	}

}
