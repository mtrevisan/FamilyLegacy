package io.github.mtrevisan.familylegacy.v2.io.model.readers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;


/**
 * Handler for TEXT VALUE VARIANT structures.
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
public final class TextValueVariantReader{

	public static final String TAG_PHONETIC = "phonetic";
	public static final String TAG_PHONETIC_SYSTEM = "system";
	public static final String TAG_PHONETIC_VALUE = "value";
	public static final String TAG_TRANSCRIPTION = "transcription";
	public static final String TAG_TRANSCRIPTION_SYSTEM = "system";
	public static final String TAG_TRANSCRIPTION_TYPE = "type";
	public static final String TAG_TRANSCRIPTION_VALUE = "value";

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


	private TextValueVariantReader(){}


	public static String extractPhoneticSystem(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_PHONETIC_SYSTEM);
	}

	public static String extractPhoneticValue(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_PHONETIC_VALUE);
	}

	public static String extractTranscriptionSystem(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TRANSCRIPTION_SYSTEM);
	}

	public static String extractTranscriptionType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TRANSCRIPTION_TYPE);
	}

	public static String extractTranscriptionValue(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TRANSCRIPTION_VALUE);
	}

	public static boolean isPhonetic(final String tag){
		return TAG_PHONETIC.equals(tag);
	}

	public static boolean isTranscription(final String tag){
		return TAG_TRANSCRIPTION.equals(tag);
	}

}
