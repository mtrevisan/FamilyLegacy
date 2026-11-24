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

import java.util.Locale;


/**
 * Handler for NOTE records.
 * <p>
 * Structure:
 * <pre>
 * struct NoteStructure {
 *   title?: Text   // note title
 *   kind?: enum { general, research, story } | Text   // what the note is for; absent means general
 *   text: Text     // comments or opinions from the submitter. In markdown or HTML notes, references to records in this file are written as links to `flef:&lt;Id&gt;` (e.g. `[Giovanni](flef:I12)`), and "confidential" data as `[text](flef:confidential)`, that marks text that SHOULD be hidden when confidential information is suppressed.
 *   mime?: Text    // e.g., text/html, text/markdown
 *   locale?: LocaleCode
 *   translation*: struct {
 *     text: Text   // a translated version of the note
 *     locale?: LocaleCode
 *   }
 *   source*: SourceCitation
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class NoteReader{

	public static final String TYPE = "NOTE";

	public static final String TAG_NOTE = "note";
	public static final String TAG_TITLE = "title";
	public static final String TAG_KIND = "kind";
	public static final String TAG_TEXT = "text";
	public static final String TAG_MIME = "mime";
	public static final String TAG_LOCALE = "locale";
	public static final String TAG_TRANSLATION = "translation";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	/** An ordinary note (what a note without a kind is). */
	public static final String ENUM_KIND_GENERAL = "general";
	/** Reasoning, findings and comments of the research: working material. */
	public static final String ENUM_KIND_RESEARCH = "research";
	/** Narrative prose meant to be read and published: it is not evidence, nor research. */
	public static final String ENUM_KIND_STORY = "story";
	public static final String[] KINDS = new String[]{ENUM_KIND_GENERAL, ENUM_KIND_RESEARCH, ENUM_KIND_STORY};

	// basic notes
	public static final String TAG_DATE = "date";


	private NoteReader(){}

	/** @return whether the record is of this type (its tag is the type, ignoring case) */
	public static boolean isOfType(final FLEFRecord record){
		return (record != null && TYPE.equalsIgnoreCase(record.getTag()));
	}



	/**
	 * @return	What the note is for: {@link #ENUM_KIND_GENERAL} if it has no kind, otherwise the kind as it is written (lowercase); it may be a
	 * 	custom one.
	 */
	public static String extractKind(final FLEFRecord record){
		final String kind = FLEFRecordHelper.getChildValue(record, TAG_KIND);
		return (kind == null || kind.isBlank()? ENUM_KIND_GENERAL: kind.strip().toLowerCase(Locale.ROOT));
	}

	/** @return	Whether the note is a story: narrative prose meant to be read and published. */
	public static boolean isStory(final FLEFRecord record){
		return ENUM_KIND_STORY.equals(extractKind(record));
	}

	/** @return	Whether the note is working material of the research. */
	public static boolean isResearch(final FLEFRecord record){
		return ENUM_KIND_RESEARCH.equals(extractKind(record));
	}

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
