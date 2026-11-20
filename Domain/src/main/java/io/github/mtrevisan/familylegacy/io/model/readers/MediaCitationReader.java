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

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;
import org.apache.commons.lang3.StringUtils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;


/**
 * Handler for MEDIA CITATION structures.
 * <p>
 * Structure:
 * <pre>
 * // A particular use of a MediaRecord by the record that contains the citation (an individual, a group, a place, an event or a repository).
 * struct MediaCitation {
 *   media: Xref&lt;MediaRecord&gt;
 *   caption?: Text      // what the media shows, in the context of the citing record
 *   crop?: CropRect     // region of the image or page, in pixels
 *   page?: Int          // 1-based page, for multi-page documents (e.g. PDF, TIFF); CROP refers to this page
 * }
 * </pre>
 * The citations are children of the citing record with the tag {@link #TAG}.
 */
public final class MediaCitationReader{

	/** The tag of the citations, in the records that can have them. */
	public static final String TAG = "media";
	public static final String TAG_MEDIA = "media";
	public static final String TAG_CAPTION = "caption";
	public static final String TAG_CROP = CropReader.TAG_CROP;
	public static final String TAG_PAGE = "page";


	/**
	 * One citation of a media, resolved.
	 *
	 * @param mediaId the id of the media
	 * @param media   the media record ({@code null} if it does not exist)
	 * @param caption the caption ({@code null} if none)
	 * @param crop    the region of the image ({@code null} if the whole image)
	 * @param page    the 1-based page ({@code null} if none)
	 */
	public record MediaUse(String mediaId, FLEFRecord media, String caption, Rectangle crop, Integer page){

		/** The URI of the media, {@code null} if the media does not exist. */
		public String uri(){
			return MediaReader.extractUri(media);
		}

		/** Whether the media is an image (a missing kind is inferred from the URI). */
		public boolean isImage(){
			final String uri = uri();
			if(uri == null)
				return false;

			final String kind = MediaReader.extractKind(media);
			return MediaReader.ENUM_KIND_IMAGE.equals(kind != null? kind: MediaReader.inferKind(uri));
		}

	}


	private MediaCitationReader(){}


	/**
	 * @param record the record that contains the citations (may be {@code null})
	 * @return the citations of the record whose media exists, in order
	 */
	public static List<MediaUse> extractAll(final FLEFRecord record, final RecordLookup model){
		final List<MediaUse> uses = new ArrayList<>(0);
		if(record == null)
			return uses;

		for(final FLEFRecord citation : FLEFRecordHelper.findChildren(record, TAG)){
			final String mediaId = FLEFRecordHelper.getChildValue(citation, TAG_MEDIA);
			if(StringUtils.isBlank(mediaId))
				continue;

			final FLEFRecord media = model.getRecordById(mediaId);
			if(media == null)
				continue;

			final String caption = StringUtils.trimToNull(FLEFRecordHelper.getChildValue(citation, TAG_CAPTION));
			final Rectangle crop = CropReader.extractPreferredImageCrop(FLEFRecordHelper.findChild(citation, TAG_CROP));
			uses.add(new MediaUse(mediaId, media, caption, crop, parsePage(FLEFRecordHelper.getChildValue(citation, TAG_PAGE))));
		}
		return uses;
	}

	private static Integer parsePage(final String value){
		try{
			final int page = (value != null? Integer.parseInt(value.trim()): 0);
			return (page >= 1? page: null);
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}

}
