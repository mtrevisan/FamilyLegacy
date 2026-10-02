package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;


/**
 * Handler for NOTE records.
 * <p>
 * Structure:
 * <pre>
 * struct NoteStructure {
 *   title?: Text   // note title
 *   text: Text     // comments or opinions from the submitter. If markdown, references to structures in this file can be written as `[text](@id@)`, "confidential" data can be written as `[text](confidential)`, and this last marks text that SHOULD be hidden when confidential information is suppressed.
 *   mime?: Text    // e.g., text/html, text/markdown
 *   locale?: LocaleCode | Text
 *   translation*: struct {
 *     text: Text   // a translated version of the note
 *     locale?: LocaleCode | Text
 *   }
 *   source*: SourceCitation
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class NoteReader{

	public static final String TAG_TITLE = "title";
	public static final String TAG_TEXT = "text";
	public static final String TAG_MIME = "mime";
	public static final String TAG_LOCALE = "locale";
	public static final String TAG_TRANSLATION = "translation";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	// basic notes
	public static final String TAG_DATE = "date";


	private NoteReader(){}


	public static String extractText(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TEXT);
	}

	public static String extractLocale(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_LOCALE);
	}

	public static String extractDate(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_DATE);
	}

}
