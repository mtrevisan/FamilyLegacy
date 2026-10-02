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
import io.github.mtrevisan.familylegacy.io.model.readers.names.Name;
import io.github.mtrevisan.familylegacy.io.model.readers.names.NameAnatomyService;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


/**
 * Handler for REPOSITORY records.
 * <p>
 * Structure:
 * <pre>
 * // A representation of where a source or set of sources is located. May be formal, like a library, or informal, like the owner of a family book.
 * record RepositoryRecord {
 *   name+: NameStructure                 // the official name of the archive in which the stated source material is stored
 *   custodian?: Xref<IndividualRecord>   // the individual acting as custodian of the repository or its source material
 *   place?: PlaceCitation                // the location of the repository
 *   contact*: ContactStructure
 *   note*: NoteStructure
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class RepositoryReader{

	public static final String TAG_NAME = "name";
	public static final String TAG_CUSTODIAN = "custodian";
	public static final String TAG_PLACE = "place";
	public static final String TAG_CONTACT = "contact";
	public static final String TAG_NOTE = "note";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";


	private RepositoryReader(){}


	/**
	 * Extracts names from an RepositoryRecord.
	 *
	 * @param repository the RepositoryRecord
	 * @return a list of name strings with excluded parts omitted
	 */
	public static List<String> extractNames(final FLEFRecord repository){
		final List<Name> names = NameAnatomyService.extractForGeneric(repository);
		return names.stream()
			.map(Name::value)
			.filter(StringUtils::isNotEmpty)
			.toList();
	}

	public static String extractPrimaryName(final FLEFRecord repository){
		final List<String> names = extractNames(repository);
		return (!names.isEmpty()? names.getFirst(): null);
	}

	public static String extractCustodian(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_CUSTODIAN);
	}

}
