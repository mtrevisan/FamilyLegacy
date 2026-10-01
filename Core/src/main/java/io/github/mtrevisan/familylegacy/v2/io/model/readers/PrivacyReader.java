package io.github.mtrevisan.familylegacy.v2.io.model.readers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;


/**
 * Handler for PRIVACY records.
 * <p>
 * Structure:
 * <pre>
 * // Specifies how this structure may be displayed, exported, or distributed. The privacy expresses the intent of the data contributor and should be
 * // respected regardless of any automatic privacy policy applied by the receiving system.
 * struct PrivacyStructure {
 *   level: enum {
 *     public,        // information may be freely displayed, exported, and distributed
 *     restricted,    // information should be treated with caution and may be omitted from public reports, exports, or distributions
 *     confidential   // information should not normally be displayed, exported, or included in public distributions
 *   }
 *   reason?: enum {   // Standardized reason for the restriction. Custom values MAY be supplied for application-specific needs. REASON SHOULD be omitted when LEVEL is public.
 *     living_person, privacy_law, copyright, repository_license, sensitive_information
 *   } | Text
 *   expires?: Date    // the date this restriction expires
 * }
 * </pre>
 */
public final class PrivacyReader{

	public static final String TAG_LEVEL = "level";
	public static final String TAG_REASON = "reason";
	public static final String TAG_EXPIRES = "expires";

	private static final String ENUM_LEVEL_PUBLIC = "public";
	private static final String ENUM_LEVEL_RESTRICTED = "restricted";
	private static final String ENUM_LEVEL_CONFIDENTIAL = "confidential";
	public static final String[] LEVELS = {
		ENUM_LEVEL_PUBLIC,
		ENUM_LEVEL_RESTRICTED,
		ENUM_LEVEL_CONFIDENTIAL
	};

	public static final String[] REASONS = {
		"living_person",
		"privacy_law",
		"copyright",
		"repository_license",
		"sensitive_information"
	};


	private PrivacyReader(){}


	public static String extractLevel(final FLEFRecord record){
		final String level = FLEFRecordHelper.getChildValue(record, TAG_LEVEL);
		return (ENUM_LEVEL_RESTRICTED.equals(level) || ENUM_LEVEL_CONFIDENTIAL.equals(level)
			? level
			: ENUM_LEVEL_PUBLIC);
	}

	public static boolean isPublic(final String level){
		return level.equals(ENUM_LEVEL_PUBLIC);
	}

	public static boolean isRestricted(final String level){
		return level.equals(ENUM_LEVEL_RESTRICTED);
	}

	public static boolean isConfidential(final String level){
		return level.equals(ENUM_LEVEL_CONFIDENTIAL);
	}

	public static String extractReason(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_REASON);
	}

	public static String extractExpires(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_EXPIRES);
	}

}
