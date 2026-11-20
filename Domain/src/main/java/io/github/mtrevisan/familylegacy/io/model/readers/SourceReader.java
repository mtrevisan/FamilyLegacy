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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Handler for SOURCE records.
 * <p>
 * Structure:
 * <pre>
 * // A source from which genealogical information is derived. A source may represent a document, publication, archive record, inscription, oral
 * // testimony, database entry, image, audio recording, or any other evidence-bearing artifact.
 * // DERIVED_FROM relationships MUST NOT form cycles: a source cannot be derived from itself, directly or through other sources.
 * record SourceRecord {
 *   title+: NameStructure             // the title of the work, record, or item and, when appropriate, the title of the larger work or series of which it is a part
 *   author*: Agent                    // the person, agency, or entity who created the record. For a published work, this could be the author, compiler, transcriber, abstractor, or editor. For an unpublished source, this may be an individual, a government agency, church organization, or private organization, etc.
 *   publisher?: Text                  // name of the publisher of the publication
 *   date?: DateStructure              // the date this source was created
 *   place?: PlaceCitation             // the place this source was created
 *   media_type?: enum {
 *     audio, book, card, electronic, fiche, film, magazine, manuscript, map, newspaper, photo, tombstone, video
 *   } | Text
 *   kind?: enum {                     // what the source IS (its genre), as opposed to media_type, the physical form of the carrier
 *     civil_registration, church_register, census, deed, will_probate, court_record, military_record, school_record, immigration, tax_roll, directory, newspaper, periodical, book, cemetery_record, correspondence, diary, oral, website, map, photograph, artwork, audio, video, manuscript, patent, identification, compilation, artifact
 *   } | Text
 *   identifier*: IdentifierStructure  // identifies the source itself (ISBN, ISSN, DOI, URL, microfilm number, ...); where it is kept is the identifier of the RepositoryCitation
 *   repository*: RepositoryCitation
 *   media*: Xref&lt;MediaRecord&gt;        // the media (scans, photographs, recordings, ...) of this source
 *   derived_from*: SourceCitation     // the sources this one was made from, when it is not an original: a transcription, an abstract, an index, a copy, a compilation or a database taken from a register, a book or other sources. The locator of the citation says where in the original the content comes from (page, entry, folio) and the extract the passage; the stance is `contradicts` when this source disagrees with its original (e.g., a mistaken transcription). An original source has none. Sources derived from the same original are not independent evidence of what it says. This is about where the content comes from: a source that only mentions other works (a bibliography, a note that refers to a source) does not derive from them
 *   note*: NoteStructure              // may contain information to identify a book (ISBN code, ...), a digital archive (website name, creator, ...), a microfilm (record title, record file, collection, film UUID, roll number, ...), etc.
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 *
 * Agent = oneof {
 *  individual: Xref&lt;IndividualRecord&gt;   // the author is an individual described in this file
 *  group: Xref&lt;GroupRecord&gt;             // the author is a group or organization described in this file (e.g. a parish, a confraternity)
 *  text: Text                           // the author is not described in this file (e.g. a publisher's editor, an unidentified clerk)
 * }
 * </pre>
 */
public final class SourceReader{

	public static final String TYPE = "SOURCE";
	public static final String ID_PREFIX = "S";

	public static final String TAG_TITLE = "title";
	/** The sub-tag holding the text of a title. */
	public static final String TAG_TITLE_VALUE = "value";
	public static final String TAG_AUTHOR = "author";
	public static final String TAG_PUBLISHER = "publisher";
	public static final String TAG_DATE = "date";
	public static final String TAG_PLACE = "place";
	public static final String TAG_MEDIA_TYPE = "media_type";
	public static final String TAG_KIND = "kind";
	public static final String TAG_IDENTIFIER = IdentifierReader.TAG_IDENTIFIER;
	public static final String TAG_REPOSITORY = "repository";
	public static final String TAG_MEDIA = "media";
	public static final String TAG_DERIVED_FROM = "derived_from";
	public static final String TAG_NOTE = "note";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String ENUM_MEDIA_TYPE_AUDIO = "audio";
	public static final String ENUM_MEDIA_TYPE_BOOK = "book";
	public static final String ENUM_MEDIA_TYPE_CARD = "card";
	public static final String ENUM_MEDIA_TYPE_ELECTRONIC = "electronic";
	public static final String ENUM_MEDIA_TYPE_FICHE = "fiche";
	public static final String ENUM_MEDIA_TYPE_FILM = "film";
	public static final String ENUM_MEDIA_TYPE_MAGAZINE = "magazine";
	public static final String ENUM_MEDIA_TYPE_MANUSCRIPT = "manuscript";
	public static final String ENUM_MEDIA_TYPE_MAP = "map";
	public static final String ENUM_MEDIA_TYPE_NEWSPAPER = "newspaper";
	public static final String ENUM_MEDIA_TYPE_PHOTO = "photo";
	public static final String ENUM_MEDIA_TYPE_TOMBSTONE = "tombstone";
	public static final String ENUM_MEDIA_TYPE_VIDEO = "video";
	public static final String[] MEDIA_TYPES = new String[]{
		ENUM_MEDIA_TYPE_AUDIO, ENUM_MEDIA_TYPE_BOOK, ENUM_MEDIA_TYPE_CARD, ENUM_MEDIA_TYPE_ELECTRONIC, ENUM_MEDIA_TYPE_FICHE, ENUM_MEDIA_TYPE_FILM,
		ENUM_MEDIA_TYPE_MAGAZINE, ENUM_MEDIA_TYPE_MANUSCRIPT, ENUM_MEDIA_TYPE_MAP, ENUM_MEDIA_TYPE_NEWSPAPER, ENUM_MEDIA_TYPE_PHOTO,
		ENUM_MEDIA_TYPE_TOMBSTONE, ENUM_MEDIA_TYPE_VIDEO
	};

	public static final String ENUM_KIND_CIVIL_REGISTRATION = "civil_registration";
	public static final String ENUM_KIND_CHURCH_REGISTER = "church_register";
	public static final String ENUM_KIND_CENSUS = "census";
	public static final String ENUM_KIND_DEED = "deed";
	public static final String ENUM_KIND_WILL_PROBATE = "will_probate";
	public static final String ENUM_KIND_COURT_RECORD = "court_record";
	public static final String ENUM_KIND_MILITARY_RECORD = "military_record";
	public static final String ENUM_KIND_SCHOOL_RECORD = "school_record";
	public static final String ENUM_KIND_IMMIGRATION = "immigration";
	public static final String ENUM_KIND_TAX_ROLL = "tax_roll";
	public static final String ENUM_KIND_DIRECTORY = "directory";
	public static final String ENUM_KIND_NEWSPAPER = "newspaper";
	public static final String ENUM_KIND_PERIODICAL = "periodical";
	public static final String ENUM_KIND_BOOK = "book";
	public static final String ENUM_KIND_CEMETERY_RECORD = "cemetery_record";
	public static final String ENUM_KIND_CORRESPONDENCE = "correspondence";
	public static final String ENUM_KIND_DIARY = "diary";
	public static final String ENUM_KIND_ORAL = "oral";
	public static final String ENUM_KIND_WEBSITE = "website";
	public static final String ENUM_KIND_MAP = "map";
	public static final String ENUM_KIND_PHOTOGRAPH = "photograph";
	public static final String ENUM_KIND_ARTWORK = "artwork";
	public static final String ENUM_KIND_AUDIO = "audio";
	public static final String ENUM_KIND_VIDEO = "video";
	public static final String ENUM_KIND_MANUSCRIPT = "manuscript";
	public static final String ENUM_KIND_PATENT = "patent";
	public static final String ENUM_KIND_IDENTIFICATION = "identification";
	public static final String ENUM_KIND_COMPILATION = "compilation";
	public static final String ENUM_KIND_ARTIFACT = "artifact";
	/** What a source IS (its documentary genre), as opposed to {@link #MEDIA_TYPES}, the physical form of its carrier. */
	public static final String[] KINDS = new String[]{
		ENUM_KIND_CIVIL_REGISTRATION, ENUM_KIND_CHURCH_REGISTER, ENUM_KIND_CENSUS, ENUM_KIND_DEED, ENUM_KIND_WILL_PROBATE, ENUM_KIND_COURT_RECORD,
		ENUM_KIND_MILITARY_RECORD, ENUM_KIND_SCHOOL_RECORD, ENUM_KIND_IMMIGRATION, ENUM_KIND_TAX_ROLL, ENUM_KIND_DIRECTORY, ENUM_KIND_NEWSPAPER,
		ENUM_KIND_PERIODICAL, ENUM_KIND_BOOK, ENUM_KIND_CEMETERY_RECORD, ENUM_KIND_CORRESPONDENCE, ENUM_KIND_DIARY, ENUM_KIND_ORAL, ENUM_KIND_WEBSITE,
		ENUM_KIND_MAP, ENUM_KIND_PHOTOGRAPH, ENUM_KIND_ARTWORK, ENUM_KIND_AUDIO, ENUM_KIND_VIDEO, ENUM_KIND_MANUSCRIPT, ENUM_KIND_PATENT,
		ENUM_KIND_IDENTIFICATION, ENUM_KIND_COMPILATION, ENUM_KIND_ARTIFACT
	};


	private SourceReader(){}

	/** @return whether the record is of this type (its tag is the type, ignoring case) */
	public static boolean isOfType(final FLEFRecord record){
		return (record != null && TYPE.equalsIgnoreCase(record.getTag()));
	}



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

	/**
	 * Text that identifies a source to the user: the first title followed by the ID in brackets.
	 *
	 * @param source the SourceRecord, with an ID
	 * @return the text
	 */
	public static String displayText(final FLEFRecord source){
		final String id = source.getId();
		final List<String> titles = extractTitles(source);
		return (!titles.isEmpty()? titles.getFirst() + " [" + id + "]": "[" + id + "]");
	}

	public static String extractPrimaryTitle(final FLEFRecord source){
		final List<String> names = extractTitles(source);
		return (!names.isEmpty()? names.getFirst(): null);
	}

	public static String extractDocument(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_MEDIA);
	}

	public static String extractAuthor(final FLEFRecord record){
		final FLEFRecord agentNode = FLEFRecordHelper.findChild(record, TAG_AUTHOR);
		return (agentNode != null? AgentReader.extractAgent(agentNode): null);
	}

	public static String extractPublisher(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_PUBLISHER);
	}

	public static String extractMediaType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_MEDIA_TYPE);
	}

	/** @return the documentary genre of the source (one of {@link #KINDS} or free text), or {@code null} if not set */
	public static String extractKind(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_KIND);
	}

	/**
	 * @param record	The SourceRecord.
	 * @return	The identifiers of the source itself (ISBN, DOI, ...), in the order of the record; not where it is kept in a repository
	 * 	(that is the identifier of its repository citation).
	 */
	public static List<FLEFRecord> extractIdentifiers(final FLEFRecord record){
		return IdentifierReader.extractIdentifiers(record);
	}

	/**
	 * @param source	The SourceRecord.
	 * @return	The identifiers of the sources it is derived from (its {@code derived_from} citations), in the order of the record; the
	 * 	citations that name no source are left out.
	 */
	public static List<String> extractDerivedFromIds(final FLEFRecord source){
		final List<String> ids = new ArrayList<>(0);
		for(final FLEFRecord citation : FLEFRecordHelper.findChildren(source, TAG_DERIVED_FROM)){
			final String id = FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_SOURCE);
			if(StringUtils.isNotBlank(id))
				ids.add(id);
		}
		return ids;
	}

}
