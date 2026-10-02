package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;


/**
 * Handler for AUDIT records.
 * <p>
 * Structure:
 * <pre>
 * // Audit trail describing when the structure was created and subsequently modified. This information concerns the record itself, not the historical
 * // subject represented by the record.
 * struct AuditStructure {
 *   creation: struct {
 *     date: Date   // the date of the creation of this structure
 *     comment?: Text
 *   }
 *   update*: struct {
 *     date: Date   // the date of the changing of this structure
 *     comment?: Text
 *   }
 * }
 * </pre>
 */
public final class AuditReader{

	public static final String TAG_CREATION = "creation";
	public static final String TAG_DATE = "date";
	public static final String TAG_COMMENT = "comment";
	public static final String TAG_CREATION_DATE = FLEFRecordHelper.composePath(TAG_CREATION, TAG_DATE);
	public static final String TAG_CREATION_COMMENT = FLEFRecordHelper.composePath(TAG_CREATION, TAG_COMMENT);
	public static final String TAG_UPDATE = "update";


	private AuditReader(){}

}
