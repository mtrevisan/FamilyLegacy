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
package io.github.mtrevisan.familylegacy.v2.io.model.readers.names;


import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;


/**
 * A phonetic or transcription variant of a name or name part.
 * <p>
 * Mirrors the FLEF {@code TextValueVariant} oneof. The {@code kind} field
 * distinguishes the two alternatives:
 * <ul>
 *   <li>{@code "phonetic"} — a phonetic or phonemic representation, with
 *       a {@code system} such as {@code IPA} or {@code canIPA};</li>
 *   <li>{@code "transcription"} — a transliteration or transcription into
 *       another writing system, with a {@code system} (e.g. {@code rōmaji},
 *       {@code pinyin}, {@code iso9}) and an optional {@code type}
 *       (e.g. {@code romanized}, {@code anglicized}, {@code hellenized}).</li>
 * </ul>
 * The record is immutable. Empty fields are normalized to empty strings.
 * <p>
 * Structure:
 * <pre>
 * // Alternative written, phonetic, transliterated, or transcribed representations of the same textual value. These representations do not constitute separate
 * // names; they are alternate renderings of the same name.
 * TextValueVariant = oneof {
 *   phonetic: struct {
 *     system: Text   // usually IPA, but other systems can be used (e.g., canIPA)
 *     value: Text    // phonetic/phonematic component
 *   }
 *   transcription: struct {
 *     system: enum {   // indicates the system used to transcribe the text to the transcribed variation
 *       rōmaji, hepburn, kunreishiki, nihonshiki,   // Japanese
 *       pinyin, wadegiles,                          // Chinese
 *       bgn_pcgn,                                   // various geographic standards
 *       iso9,                                       // Cyrillic -> Latin
 *       ala_lc,                                     // Library of Congress
 *       dmg,                                        // Arabic/Persian scholarly
 *       buckwalter,                                 // Arabic
 *       iso233,                                     // Arabic
 *       iso259,                                     // Hebrew
 *       iast,                                       // Sanskrit
 *       iso15919, hunterian,                        // Indic scripts
 *       mccune_reischauer, revised_korean,          // Korean
 *       scientific                                  // generic scholarly transliteration
 *     } | Text
 *     type?: enum {
 *       romanized,      // converted to Latin script
 *       latinized,      // converted to Latinized scholarly form
 *       anglicized,     // adapted to English
 *       francized,      // adapted to French
 *       germanized,     // adapted to German
 *       italianized,    // adapted to Italian
 *       hispanicized,   // adapted to Spanish
 *       lusitanized,    // adapted to Portuguese
 *       cyrillized,     // converted to Cyrillic
 *       arabized,       // converted to Arabic script
 *       hebraized,      // converted to Hebrew script
 *       hellenized,     // converted to Greek script
 *       gairaigized,    // adapted to Japanese loanword form
 *       modernized,     // historic spelling modernized
 *       normalized      // orthography normalized
 *     } | Text
 *     value: Text      // transcribed component
 *   }
 * }
 * </pre>
 */
public record TextValueVariant(String kind, String system, String type, String value){

	private static final String TAG_PHONETIC = "phonetic";
	private static final String TAG_PHONETIC_SYSTEM = "system";
	private static final String TAG_PHONETIC_VALUE = "value";
	private static final String TAG_TRANSCRIPTION = "transcription";
	private static final String TAG_TRANSCRIPTION_SYSTEM = "system";
	private static final String TAG_TRANSCRIPTION_TYPE = "type";
	private static final String TAG_TRANSCRIPTION_VALUE = "value";

	public static final String[] TRANSCRIPTION_SYSTEMS = {
		StringUtils.EMPTY,
		"romaji", "hepburn", "kunreishiki", "nihonshiki",
		"pinyin", "wadegiles",
		"bgn_pcgn",
		"iso9",
		"ala_lc",
		"dmg",
		"buckwalter",
		"iso233",
		"iso259",
		"iast",
		"iso15919", "hunterian",
		"mccune_reischauer", "revised_korean",
		"scientific"
	};
	public static final String[] TRANSCRIPTION_TYPES = {
		StringUtils.EMPTY,
		"romanized", "latinized", "anglicized", "francized", "germanized", "italianized", "hispanicized",
		"lusitanized", "cyrillized", "arabized", "hebraized", "hellenized", "gairaigized", "modernized", "normalized"
	};


	public TextValueVariant{
		if(kind == null)
			kind = StringUtils.EMPTY;
		if(system == null)
			system = StringUtils.EMPTY;
		if(type == null)
			type = StringUtils.EMPTY;
		if(value == null)
			value = StringUtils.EMPTY;
	}


	public static TextValueVariant create(final FLEFRecord kindChild){
		final String kind = kindChild.getTag();
		if(isPhonetic(kindChild)){
			final String system = FLEFRecordHelper.getChildValue(kindChild, TAG_PHONETIC_SYSTEM);
			final String value = FLEFRecordHelper.getChildValue(kindChild, TAG_PHONETIC_VALUE);
			return new TextValueVariant(kind, system, null, value);
		}
		else if(isTranscription(kindChild)){
			final String system = FLEFRecordHelper.getChildValue(kindChild, TAG_TRANSCRIPTION_SYSTEM);
			final String type = FLEFRecordHelper.getChildValue(kindChild, TAG_TRANSCRIPTION_TYPE);
			final String value = FLEFRecordHelper.getChildValue(kindChild, TAG_TRANSCRIPTION_VALUE);
			return new TextValueVariant(kind, system, type, value);
		}
		return null;
	}


	/**
	 * Returns whether this variant is a phonetic representation.
	 *
	 * @return {@code true} if the kind is {@link #TAG_PHONETIC}
	 */
	public boolean isPhonetic(){
		return TAG_PHONETIC.equals(kind);
	}

	public static boolean isPhonetic(final FLEFRecord record){
		return TAG_PHONETIC.equals(record.getTag());
	}

	/**
	 * Returns whether this variant is a transcription or transliteration.
	 *
	 * @return {@code true} if the kind is {@link #TAG_TRANSCRIPTION}
	 */
	public boolean isTranscription(){
		return TAG_TRANSCRIPTION.equals(kind);
	}

	public static boolean isTranscription(final FLEFRecord record){
		return TAG_TRANSCRIPTION.equals(record.getTag());
	}

	/**
	 * Returns a compact display label for the variant, combining the
	 * system and the type when both are present.
	 *
	 * @return the display label, never {@code null}
	 */
	public String displayLabel(){
		final StringBuilder sb = new StringBuilder();
		if(!system.isEmpty())
			sb.append(system);
		if(!type.isEmpty()){
			if(!sb.isEmpty())
				sb.append(" / ");
			sb.append(type);
		}
		if(sb.isEmpty())
			sb.append(kind);
		return sb.toString();
	}

}
