package io.github.mtrevisan.familylegacy.v2.io.model.readers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;


/**
 * Handler for EVIDENCE QUALIFIERS records.
 * <p>
 * Structure:
 * <pre>
 * // Qualifiers describing the evidentiary assessment of a specific assertion.
 * // These qualifiers apply to the assertion being cited and do not express the reliability, authority, or quality of the source as a whole.
 * struct EvidenceQualifiers {
 *   source_type?: enum {        // Classification of the source itself.
 *     original, derived
 *   }
 *   information_type?: enum {   // Classification of the information provided by the source.
 *     primary, secondary, undetermined
 *   }
 *   evidence_type?: enum {      // Nature of the evidentiary contribution toward the assertion.
 *     direct, indirect, negative
 *   }
 * }
 * </pre>
 */
public final class EvidenceQualifiersReader{

	public static final String TAG_SOURCE_TYPE = "source_type";
	public static final String TAG_INFORMATION_TYPE = "information_type";
	public static final String TAG_EVIDENCE_TYPE = "evidence_type";

	public static final String[] SOURCE_TYPES = {
		"original", "derived"
	};

	public static final String INFORMATION_TYPE_PRIMARY = "primary";
	public static final String INFORMATION_TYPE_SECONDARY = "secondary";
	public static final String INFORMATION_TYPE_UNDETERMINED = "undetermined";
	public static final String[] INFORMATION_TYPES = {
		INFORMATION_TYPE_PRIMARY, INFORMATION_TYPE_SECONDARY, INFORMATION_TYPE_UNDETERMINED
	};

	public static final String[] EVIDENCE_TYPES = {
		"direct", "indirect", "negative"
	};


	private EvidenceQualifiersReader(){}


	public static String extractSourceType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_SOURCE_TYPE);
	}

	public static String extractInformationType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_INFORMATION_TYPE);
	}

	public static String extractEvidenceType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_EVIDENCE_TYPE);
	}

}
