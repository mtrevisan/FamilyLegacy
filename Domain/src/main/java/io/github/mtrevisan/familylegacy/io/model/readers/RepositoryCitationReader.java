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

import java.util.List;


/**
 * Handler for REPOSITORY CITATION records.
 * <p>
 * Structure:
 * <pre>
 * // Citation of a repository holding a source or source collection. Allows recording repository-specific shelf marks, call numbers,
 * // classification numbers, and other retrieval information.
 * struct RepositoryCitation {
 *   repository: Xref&lt;RepositoryRecord&gt;
 *   identifier*: IdentifierStructure   // where the source can be found within this repository (shelf mark, call number, Dewey number, LCC, ...)
 *   locator?: Text   // position inside the source itself (e.g., 'Page: 27', 'Entry: 45', 'Folio: 8r', 'Frame: 213', or 'Volume: 2, Page: 143, Entry: 17')
 *   remark*: Text
 * }
 * </pre>
 */
public final class RepositoryCitationReader{

	public static final String TYPE = "REPOSITORY_CITATION";

	public static final String TAG_REPOSITORY = "repository";
	public static final String TAG_IDENTIFIER = IdentifierReader.TAG_IDENTIFIER;
	public static final String TAG_LOCATOR = "locator";
	public static final String TAG_REMARK = "remark";


	private RepositoryCitationReader(){}


	/**
	 * @param record	The RepositoryCitation.
	 * @return	The identifiers that say where the source is found in this repository (shelf mark, call number, classification number), in
	 * 	the order of the record. The position inside the source is the locator, not an identifier.
	 */
	public static List<FLEFRecord> extractIdentifiers(final FLEFRecord record){
		return IdentifierReader.extractIdentifiers(record);
	}


	public static String extractRepository(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_REPOSITORY);
	}

}
