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
 * Handler for PLACE RELATIONSHIP records.
 * <p>
 * Structure:
 * <pre>
 * // A relationship connecting two place records. Place relationships are used to represent geographic, administrative, ecclesiastical, judicial,
 * // cadastral, postal, or historical associations between places. These relationships may change over time and can therefore be bounded by
 * // validity dates.
 * // For each relationship type, implementations SHOULD prevent cycles. Cycle validation SHOULD take validity dates into account when present.
 * // Relationships are directional: `administrative_part_of(Treviso -> Veneto)` is NOT equivalent to `administrative_part_of(Veneto -> Treviso)`.
 * record PlaceRelationshipRecord {
 *   subject: struct {
 *     place: Xref&lt;PlaceRecord&gt;   // the place for which the relationship is asserted. The SUBJECT is always the place whose role is described by TYPE relative to the TARGET.
 *   }
 *   object: struct {
 *     place: Xref&lt;PlaceRecord&gt;   // the place to which the subject is related
 *   }
 *   type: enum {
 *     administrative_part_of,   // subject is part of an administrative jurisdiction, e.g., Treviso → Veneto
 *     geographic_part_of,       // subject is geographically contained within another place
 *     ecclesiastical_part_of,   // subject belongs to an ecclesiastical jurisdiction such as a parish, diocese, or archdiocese
 *     judicial_part_of,         // subject belongs to a court, district, or judicial jurisdiction
 *     cadastral_part_of         // subject belongs to a cadastral or land-records district
 *   } | Text
 *   valid_from?: DateStructure   // the date from which this place relationship is known or believed to have been valid
 *   valid_to?: DateStructure     // the date until which this place relationship is known or believed to have been valid
 *   source*: SourceCitation      // a list of source citations supporting the existence, interpretation, or dating of this place relationship
 *   note*: NoteStructure         // a note containing commentary, interpretation, or discussion related to this place relationship
 *   evidence?: EvidenceQualifiers
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class PlaceRelationshipReader{

	public static final String TAG_SUBJECT = "subject";
	public static final String TAG_OBJECT = "object";
	public static final String TAG_TYPE = "type";
	public static final String TAG_VALID_FROM = "valid_from";
	public static final String TAG_VALID_TO = "valid_to";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_NOTE = "note";
	public static final String TAG_EVIDENCE = "evidence";
	public static final String TAG_AUDIT = "audit";

	public static final String[] TYPES = new String[]{
		"administrative_part_of", "geographic_part_of", "ecclesiastical_part_of", "judicial_part_of",
		"cadastral_part_of"
	};


	private PlaceRelationshipReader(){}


	public static String extractType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TYPE);
	}

}
