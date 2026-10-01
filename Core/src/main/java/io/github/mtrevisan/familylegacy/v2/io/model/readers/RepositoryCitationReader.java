package io.github.mtrevisan.familylegacy.v2.io.model.readers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;


/**
 * Handler for REPOSITORY CITATION records.
 * <p>
 * Structure:
 * <pre>
 * // Citation of a repository holding a source or source collection. Allows recording repository-specific shelf marks, page references,
 * // entry identifiers, or other retrieval information.
 * struct RepositoryCitation {
 *   repository: Xref&lt;RepositoryRecord&gt;
 *   locator?: Text   // location of the referenced material inside the repository (e.g., 'Shelf Mark: 12, Film: 1234567, Frame: 344, Line: 28', or 'Volume: 2, Page: 143, Entry: 17', or 'folio', 'image', etc.)
 *   note*: Text
 * }
 * </pre>
 */
public final class RepositoryCitationReader{

	public static final String TAG_REPOSITORY = "repository";
	public static final String TAG_LOCATOR = "locator";
	public static final String TAG_NOTE = "note";


	private RepositoryCitationReader(){}


	public static String extractRepository(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_REPOSITORY);
	}

}
