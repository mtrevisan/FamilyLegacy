package io.github.mtrevisan.familylegacy.v2.io.model.readers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.names.Name;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.names.NameAnatomyService;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


/**
 * Handler for SOURCE records.
 * <p>
 * Structure:
 * <pre>
 * // A source from which genealogical information is derived. A source may represent a document, publication, archive record, inscription, oral
 * // testimony, database entry, image, audio recording, or any other evidence-bearing artifact.
 * // Citation relationships SHOULD NOT form cycles.
 * record SourceRecord {
 *   title+: NameStructure             // the title of the work, record, or item and, when appropriate, the title of the larger work or series of which it is a part
 *   author?: Text                     // the person, agency, or entity who created the record. For a published work, this could be the author, compiler, transcriber, abstractor, or editor. For an unpublished source, this may be an individual, a government agency, church organization, or private organization, etc.
 *   publisher?: Text                  // name of the publisher of the publication
 *   date?: DateStructure              // the date this source was created
 *   place?: PlaceCitation             // the place this source was created
 *   media_type?: enum {
 *     audio, book, card, electronic, fiche, film, magazine, manuscript, map, newspaper, photo, tombstone, video
 *   } | Text
 *   repository*: RepositoryCitation
 *   document*: Xref&lt;DocumentRecord&gt;   // a document reference to the auxiliary data to be linked to this context
 *   note*: NoteStructure              // may contain information to identify a book (ISBN code, ...), a digital archive (website name, creator, ...), a microfilm (record title, record file, collection, film UUID, roll number, ...), etc.
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class SourceReader{

	public static final String TAG_TITLE = "title";
	public static final String TAG_AUTHOR = "author";
	public static final String TAG_PUBLISHER = "publisher";
	public static final String TAG_DATE = "date";
	public static final String TAG_PLACE = "place";
	public static final String TAG_MEDIA_TYPE = "media_type";
	public static final String TAG_REPOSITORY = "repository";
	public static final String TAG_DOCUMENT = "document";
	public static final String TAG_NOTE = "note";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String[] MEDIA_TYPES = new String[]{
		"audio", "book", "card", "electronic", "fiche", "film",
		"magazine", "manuscript", "map", "newspaper", "photo",
		"tombstone", "video"
	};


	private SourceReader(){}


	/**
	 * Extracts names from an SourceRecord.
	 *
	 * @param source the SourceRecord
	 * @return a list of name strings with excluded parts omitted
	 */
	public static List<String> extractTitles(final FLEFRecord source){
		final List<Name> names = NameAnatomyService.extractForSource(source);
		return names.stream()
			.map(Name::value)
			.filter(StringUtils::isNotEmpty)
			.toList();
	}

	public static String extractDocument(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_DOCUMENT);
	}

	public static String extractAuthor(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_AUTHOR);
	}

	public static String extractPublisher(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_PUBLISHER);
	}

	public static String extractMediaType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_MEDIA_TYPE);
	}

}
