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
