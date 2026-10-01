package io.github.mtrevisan.familylegacy.v2.io.model.readers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;


/**
 * Handler for DOCUMENT records.
 * <p>
 * Structure:
 * <pre>
 * // A digital or physical representation associated with a source, such as an image, PDF, scan, transcription, audio recording, video, or other media
 * // resource.
 * record DocumentRecord {
 *   uri: Uri             // URI identifying the underlying resource.
 *   mapping?: enum {     // Describes how visual image data should be interpreted and rendered.
 *     planar,                                   // Standard two-dimensional image with no panoramic or 3D projection.
 *     spherical_equirectangular,                // Full spherical panorama stored using an equirectangular projection, typically with a 2:1 aspect ratio and intended for 360° viewing.
 *     spherical_uv,                             // Image mapped directly onto a sphere using UV texture coordinates.
 *     cubemap,                                  // Image represented as six cube faces forming a complete panoramic view.
 *     cylindrical_equirectangular_horizontal,   // Cylindrical panorama wrapped horizontally around a cylinder.
 *     cylindrical_equirectangular_vertical      // Cylindrical panorama wrapped vertically around a cylinder.
 *   } | Text
 *   description?: Text   // Optional description of the document or media (e.g. "cover", "back", "page 3", "index", "cemetery entrance").
 *   note*: NoteStructure
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class DocumentReader{

	public static final String TAG_URI = "uri";
	public static final String TAG_MAPPING = "mapping";
	public static final String TAG_DESCRIPTION = "description";
	public static final String TAG_NOTE = "note";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String[] MAPPINGS = new String[]{
		"planar", "spherical_equirectangular", "spherical_uv", "cubemap", "cylindrical_equirectangular_horizontal",
		"cylindrical_equirectangular_vertical"};


	private DocumentReader(){}


	public static String extractUri(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_URI);
	}

	public static String extractMapping(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_MAPPING);
	}

	public static String extractDescription(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_DESCRIPTION);
	}

}
