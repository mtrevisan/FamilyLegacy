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

import java.util.ArrayList;
import java.util.List;


/**
 * Handler for EXTRACT records.
 * <p>
 * Structure:
 * <pre>
 * // A localized, reusable representation of source content: a transcription, transliteration, translation, summary, or exact excerpt.
 * // An ExtractRecord is NOT evidence by itself. Evidence is still expressed by SourceCitation, which may reference one or more extracts.
 * record ExtractRecord {
 *   source: Xref&lt;SourceRecord&gt;       // the source this extract belongs to
 *   media_part*: struct {
 *     media: Xref&lt;MediaRecord&gt;
 *     page?: Int
 *     crop?: CropRect
 *     start_time?: Duration
 *     end_time?: Duration
 *   }
 *   text?: Text
 *   type?: enum { verbatim, summarized, translated, normalized }
 *   locale?: LocaleCode
 *   remark*: Text
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 *
 *   require at_least_one(media_part, text)
 *   require every media_part.media in source.media
 * }
 * </pre>
 * A {@code SourceCitation} references its extracts ({@code extract*: Xref<ExtractRecord>}), and every extract it references belongs
 * to the source it cites ({@code require every e in extract: e.source == source}).
 */
public final class ExtractReader{

	public static final String TYPE = "EXTRACT";
	public static final String ID_PREFIX = "X";

	public static final String TAG_SOURCE = "source";
	public static final String TAG_MEDIA_PART = "media_part";
	public static final String TAG_MEDIA = "media";
	public static final String TAG_PAGE = "page";
	public static final String TAG_CROP = "crop";
	public static final String TAG_START_TIME = "start_time";
	public static final String TAG_END_TIME = "end_time";
	public static final String TAG_TEXT = "text";
	public static final String TAG_TYPE = "type";
	public static final String TAG_LOCALE = "locale";
	public static final String TAG_REMARK = "remark";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String TAG_MEDIA_PART_MEDIA = FLEFRecordHelper.composePath(TAG_MEDIA_PART, TAG_MEDIA);
	public static final String TAG_MEDIA_PART_CROP = FLEFRecordHelper.composePath(TAG_MEDIA_PART, TAG_CROP);

	public static final String ENUM_TYPE_VERBATIM = "verbatim";
	public static final String ENUM_TYPE_SUMMARIZED = "summarized";
	public static final String ENUM_TYPE_TRANSLATED = "translated";
	public static final String ENUM_TYPE_NORMALIZED = "normalized";
	public static final String[] TYPES = {
		ENUM_TYPE_VERBATIM,
		ENUM_TYPE_SUMMARIZED,
		ENUM_TYPE_TRANSLATED,
		ENUM_TYPE_NORMALIZED
	};


	private ExtractReader(){}


	/** @return whether the record is of this type (its tag is the type, ignoring case) */
	public static boolean isOfType(final FLEFRecord record){
		return (record != null && TYPE.equalsIgnoreCase(record.getTag()));
	}


	public static String extractSource(final FLEFRecord extract){
		return FLEFRecordHelper.getChildValue(extract, TAG_SOURCE);
	}

	public static String extractText(final FLEFRecord extract){
		return FLEFRecordHelper.getChildValue(extract, TAG_TEXT);
	}

	public static String extractType(final FLEFRecord extract){
		return FLEFRecordHelper.getChildValue(extract, TAG_TYPE);
	}

	public static String extractLocale(final FLEFRecord extract){
		return FLEFRecordHelper.getChildValue(extract, TAG_LOCALE);
	}

	public static List<String> extractRemarks(final FLEFRecord extract){
		final List<FLEFRecord> remarks = FLEFRecordHelper.findChildren(extract, TAG_REMARK);
		final List<String> values = new ArrayList<>(remarks.size());
		for(final FLEFRecord remark : remarks)
			if(remark.getValue() != null)
				values.add(remark.getValue());
		return values;
	}


	/**
	 * The extracts a citation references, in order; the references that do not resolve to an extract are skipped.
	 *
	 * @param citation	The source citation.
	 * @param lookup	Where the extracts are looked up.
	 * @return	The extract records.
	 */
	public static List<FLEFRecord> extractsOf(final FLEFRecord citation, final RecordLookup lookup){
		if(citation == null || lookup == null)
			return List.of();

		final List<FLEFRecord> references = FLEFRecordHelper.findChildren(citation, SourceCitationReader.TAG_EXTRACT);
		if(references.isEmpty())
			return List.of();

		final List<FLEFRecord> extracts = new ArrayList<>(references.size());
		for(final FLEFRecord reference : references){
			final FLEFRecord extract = lookup.getRecordById(reference.getValue());
			if(isOfType(extract))
				extracts.add(extract);
		}
		return extracts;
	}

	/**
	 * The extracts that belong to a source, in the order of the model.
	 *
	 * @param sourceId	The id of the source.
	 * @param lookup	Where the extracts are looked up.
	 * @return	The extract records whose source is the given one.
	 */
	public static List<FLEFRecord> extractsOfSource(final String sourceId, final RecordLookup lookup){
		if(sourceId == null || lookup == null)
			return List.of();

		final List<FLEFRecord> extracts = new ArrayList<>();
		for(final FLEFRecord extract : lookup.getRecordsByType(TYPE))
			if(sourceId.equals(extractSource(extract)))
				extracts.add(extract);
		return extracts;
	}

}
