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
