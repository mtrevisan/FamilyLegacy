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


/**
 * Handler for SOURCE CITATION records.
 * <p>
 * Structure:
 * <pre>
 * // Reference to a SOURCE record used as evidence for an assertion, together with optional location, extracts, and evidence classification.
 * struct SourceCitation {
 *   source: Xref&lt;SourceRecord&gt;
 *   locator?: Text   // specific location within the source referenced, in the form of a label and value pair (e.g., 'Line: 28', or 'Page: 143, Entry: 17', or 'folio', etc.)
 *   extract*: Xref&lt;ExtractRecord&gt;
 *   stance: enum { supports, contradicts }
 *   evidence?: EvidenceQualifiers
 *   privacy?: PrivacyStructure
 *
 *   require every e in extract: e.source == source
 * }
 * </pre>
 * The extracts are records of their own (see {@link ExtractReader}): every extract a citation references belongs to the source it
 * cites.
 */
public final class SourceCitationReader{

	public static final String TYPE = "SOURCE_CITATION";

	public static final String TAG_SOURCE = "source";
	public static final String TAG_LOCATOR = "locator";
	public static final String TAG_EXTRACT = "extract";
	public static final String TAG_STANCE = "stance";
	public static final String TAG_EVIDENCE = "evidence";
	public static final String TAG_PRIVACY = "privacy";

	public static final String STANCE_SUPPORTS = "supports";
	public static final String STANCE_CONTRADICTS = "contradicts";
	public static final String[] STANCES = {
		STANCE_SUPPORTS,
		STANCE_CONTRADICTS
	};

	private SourceCitationReader(){}


	public static String extractSource(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_SOURCE);
	}

	public static boolean isContradicting(final FLEFRecord citation){
		final String stance = FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_STANCE);
		return STANCE_CONTRADICTS.equals(stance);
	}

	/**
	 * How much the citations of an assertion weigh as support, counting the evidence qualifiers: each citation that supports it counts
	 * one, plus one for an original source (not a derived one), one for primary information and one for direct evidence; a citation that
	 * contradicts it, or whose evidence is negative (it supports the absence of the assertion), counts minus one. It is a rule of thumb to
	 * suggest which of several assertions is better supported, never a judgement of the sources.
	 *
	 * @param assertion	The record that carries the citations (an event, an attribute, a relationship).
	 * @return	The weight, {@code 0} if the record has no citation.
	 */
	public static int supportWeight(final FLEFRecord assertion){
		int weight = 0;
		for(final FLEFRecord citation : FLEFRecordHelper.findChildren(assertion, TAG_SOURCE)){
			final FLEFRecord evidence = FLEFRecordHelper.findChild(citation, TAG_EVIDENCE);
			if(isContradicting(citation) || EvidenceQualifiersReader.isNegative(evidence)){
				weight --;
				continue;
			}

			weight ++;
			if(evidence == null)
				continue;

			if(EvidenceQualifiersReader.ENUM_SOURCE_TYPE_ORIGINAL.equals(EvidenceQualifiersReader.extractSourceType(evidence)))
				weight ++;
			if(EvidenceQualifiersReader.INFORMATION_TYPE_PRIMARY.equals(EvidenceQualifiersReader.extractInformationType(evidence)))
				weight ++;
			if(EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_DIRECT.equals(EvidenceQualifiersReader.extractEvidenceType(evidence)))
				weight ++;
		}
		return weight;
	}

	/**
	 * Whether the record is cited by at least one source that supports it (a contradicting citation is not a backing).
	 */
	public static boolean hasSupportingCitation(final FLEFRecord record){
		for(final FLEFRecord citation : FLEFRecordHelper.findChildren(record, TAG_SOURCE))
			if(!isContradicting(citation))
				return true;
		return false;
	}

}
