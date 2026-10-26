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
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Set;


/**
 * Handler for MEDIA records.
 * <p>
 * Structure:
 * <pre>
 * // A digital or physical representation associated with a source, such as an image, PDF, scan, transcription, audio recording, video, or other media
 * // resource.
 * record MediaRecord {
 *   uri: Uri             // URI identifying the underlying resource.
 *   kind: enum {         // the nature of the resource
 *     image,      // a still image such as a photograph, scan, map, or illustration
 *     document,   // a textual or paginated document such as a PDF, transcription, OCR output, or word-processing document
 *     audio,      // an audio recording such as an interview, oral history, or speech
 *     video       // a video recording such as a documentary, interview, or family recording
 *   }
 *   mapping?: enum {     // Describes how visual image data (of an image, a video, or a page of a document) should be interpreted and rendered.
 *     planar,                                   // Standard two-dimensional image with no panoramic or 3D projection.
 *     spherical_equirectangular,                // Full spherical panorama stored using an equirectangular projection, typically with a 2:1 aspect ratio and intended for 360° viewing.
 *     spherical_uv,                             // Image mapped directly onto a sphere using UV texture coordinates.
 *     cubemap,                                  // Image represented as six cube faces forming a complete panoramic view.
 *     cylindrical_equirectangular_horizontal,   // Cylindrical panorama wrapped horizontally around a cylinder.
 *     cylindrical_equirectangular_vertical      // Cylindrical panorama wrapped vertically around a cylinder.
 *   } | Text
 *   description?: Text   // Optional description of the media (e.g. "cover", "back", "page 3", "index", "cemetery entrance").
 *   note*: NoteStructure
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class MediaReader{

	public static final String TAG_URI = "uri";
	public static final String TAG_KIND = "kind";
	public static final String TAG_MAPPING = "mapping";
	public static final String TAG_DESCRIPTION = "description";
	public static final String TAG_NOTE = "note";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String ENUM_KIND_IMAGE = "image";
	public static final String ENUM_KIND_DOCUMENT = "document";
	public static final String ENUM_KIND_AUDIO = "audio";
	public static final String ENUM_KIND_VIDEO = "video";
	public static final String[] KINDS = new String[]{ENUM_KIND_IMAGE, ENUM_KIND_DOCUMENT, ENUM_KIND_AUDIO, ENUM_KIND_VIDEO};

	public static final String[] MAPPINGS = new String[]{
		"planar", "spherical_equirectangular", "spherical_uv", "cubemap", "cylindrical_equirectangular_horizontal",
		"cylindrical_equirectangular_vertical"};

	private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "jpe", "jfif", "png", "gif", "bmp", "tif", "tiff",
		"webp", "heic", "heif", "svg", "ico", "jp2", "pcx", "psd");
	private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "wave", "ogg", "oga", "flac", "aac", "m4a", "wma",
		"aif", "aiff", "opus", "mid", "midi");
	private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "m4v", "mov", "avi", "mkv", "wmv", "webm", "mpg", "mpeg",
		"3gp", "flv", "ogv");


	private MediaReader(){}


	public static String extractUri(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_URI);
	}

	public static String extractKind(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_KIND);
	}

	public static String extractMapping(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_MAPPING);
	}

	public static String extractDescription(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_DESCRIPTION);
	}

	/**
	 * Infers the kind of a media from the extension of its URI.
	 *
	 * @param uri	The URI (or file path) of the media.
	 * @return	The kind; a resource whose extension is not of an image, an audio, or a video is a document.
	 */
	public static String inferKind(final String uri){
		if(StringUtils.isBlank(uri))
			return ENUM_KIND_DOCUMENT;

		String path = uri.trim();
		final int queryStart = StringUtils.indexOfAny(path, '?', '#');
		if(queryStart >= 0)
			path = path.substring(0, queryStart);
		final int separator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		final int dot = path.lastIndexOf('.');
		if(dot <= separator)
			return ENUM_KIND_DOCUMENT;

		final String extension = path.substring(dot + 1).toLowerCase(Locale.ROOT);
		if(IMAGE_EXTENSIONS.contains(extension))
			return ENUM_KIND_IMAGE;
		if(AUDIO_EXTENSIONS.contains(extension))
			return ENUM_KIND_AUDIO;
		if(VIDEO_EXTENSIONS.contains(extension))
			return ENUM_KIND_VIDEO;
		return ENUM_KIND_DOCUMENT;
	}

}
