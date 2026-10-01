package io.github.mtrevisan.familylegacy.v2.io.model.readers;


import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;


/**
 * Handler for SOURCE CITATION records.
 * <p>
 * Structure:
 * <pre>
 * // Reference to a SOURCE record used as evidence for an assertion, together with optional location, notes, crop coordinates, and evidence assessment.
 * struct SourceCitation {
 *   source: Xref&lt;SourceRecord&gt;
 *   locator?: Text   // specific location within the source referenced, in the form of a label and value pair (e.g., 'Line: 28', or 'Page: 143, Entry: 17', or 'folio', etc.)
 *   extract*: ExtractStructure
 *   evidence?: EvidenceQualifiers
 *   privacy?: PrivacyStructure
 *
 *   require extract.document_part.document in source.document
 * }
 *
 * struct ExtractStructure {
 *   document_part*: struct {
 *     document: Xref&lt;DocumentRecord&gt;
 *     crop?: CropRect   // Used when the citation refers to a specific region of an image rather than the entire image.
 *   }
 *   text?: Text
 *   type?: enum {   // Describes how the TEXT value relates to the content of the cited source.
 *     verbatim,     // The text is reproduced exactly as it appears in the source, preserving the original wording, spelling, abbreviations, capitalization, and punctuation. No editorial changes have been made.
 *     summarized,   // The text is a researcher-created summary of the relevant source content. It conveys the meaning of the source without reproducing the wording exactly and may omit details considered irrelevant to the citation.
 *     translated,   // The text is a translation of source content into another language. The meaning should be preserved, but the wording differs from the source because of the language conversion.
 *     normalized    // The text has been regularized for readability or analysis while remaining in the same language. Examples include expanding abbreviations, modernizing spelling, standardizing capitalization, converting archaic letter forms, or regularizing date and name formats.
 *   }
 *   locale?: LocaleCode | Text
 *   note*: Text
 *
 *   require at_least_one(document_part, text)
 * }
 *
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
public final class SourceCitationReader{

	public static final String TAG_SOURCE = "source";
	public static final String TAG_LOCATOR = "locator";
	public static final String TAG_EXTRACT = "extract";
	public static final String TAG_EVIDENCE = "evidence";
	public static final String TAG_PRIVACY = "privacy";

	public static final String TAG_DOCUMENT_PART = "document_part";
	public static final String TAG_DOCUMENT = "document";
	public static final String TAG_TEXT = "text";
	public static final String TAG_TYPE = "type";
	public static final String TAG_LOCALE = "locale";
	public static final String TAG_NOTE = "note";

	public static final String[] EXTRACT_TYPES = {
		"verbatim",
		"summarized",
		"translated",
		"normalized"
	};


	private SourceCitationReader(){}


	public static String extractSource(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_SOURCE);
	}


	public static String extractExtractText(final FLEFRecord extractRecord){
		return FLEFRecordHelper.getChildValue(extractRecord, TAG_SOURCE);
	}

	public static String extractExtractType(final FLEFRecord extractRecord){
		return FLEFRecordHelper.getChildValue(extractRecord, TAG_TYPE);
	}

	public static String extractExtractLocale(final FLEFRecord extractRecord){
		return FLEFRecordHelper.getChildValue(extractRecord, TAG_LOCALE);
	}

	public static String extractExtractNote(final FLEFRecord extractRecord){
		return FLEFRecordHelper.getChildValue(extractRecord, TAG_NOTE);
	}

}
