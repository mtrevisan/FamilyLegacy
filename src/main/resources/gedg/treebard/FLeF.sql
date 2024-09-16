-- https://treebard.com/gedcom.html#relationships
-- https://treebard.proboards.com/search/results?captcha_id=captcha_search&what_all=database+table+description&who_only_made_by=0&display_as=0&search=Search
-- https://app.sqldbm.com/PostgreSQL/DatabaseExplorer/p302632/


-- Assertion - Citation - Source - Repository

-- What the source says at the citation within the source.
CREATE TABLE "ASSERTION"
(
 "ID"            bigint PRIMARY KEY,
 CITATION_ID     bigint NOT NULL,	-- The citation from which this assertion is derived.
 JUNCTION_ID     bigint,				-- The ID of the referenced record in the table (tables can be "place", "cultural norm", "historic date", "calendar", "person", "group", "media", "person name").
 ROLE            text,					-- What role the cited entity played in the event that is being cited in this context (ex. "child", "father", "mother", "partner", "midwife", "bridesmaid", "best man", "parent", "prisoner", "religious officer", "justice of the peace", "supervisor", "employer", "employee", "witness", "assistant", "roommate", "landlady", "landlord", "foster parent", "makeup artist", "financier", "florist", "usher", "photographer", "bartender", "bodyguard", "adoptive parent", "hairdresser", "chauffeur", "treasurer", "trainer", "secretary", "navigator", "neighbor", "maid", "pilot", "undertaker", "mining partner", "legal guardian", "interior decorator", "executioner", "driver", "host", "hostess", "farm hand", "ranch hand", "junior partner", "butler", "boarder", "chef", "patent attorney").
 CERTAINTY       text,					-- A status code that allows passing on the users opinion of whether the assertion cause has really caused the assertion (ex. "impossible", "unlikely", "possible", "almost certain", "certain").
 CREDIBILITY     text,					-- A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence ("unreliable/estimated data", "questionable reliability of evidence", "secondary evidence, data officially recorded sometime after assertion", "direct and primary evidence used, or by dominance of the evidence").
 FOREIGN KEY (CITATION_ID) REFERENCES CITATION ( "ID" ) ON DELETE CASCADE,
 FOREIGN KEY (JUNCTION_ID) REFERENCES JUNCTION ( "ID" ) ON DELETE CASCADE
);
-- CREATE (a:Assertion {id: $id, role: $role, certainty: $certainty, credibility: $credibility});
-- MATCH (a:Assertion {id: $assertionID}), (c:Citation {id: $citationID})
-- CREATE (a)-[:ASSERTED_IN]->(c);
-- MATCH (a:Assertion {id: $assertionID}), (p:Place {id: $placeID})
-- CREATE (a)-[:ASSERTION_FOR]->(p);
-- MATCH (a:Assertion {id: $assertionID}), (cn:CulturalNorm {id: $culturalNormID})
-- CREATE (a)-[:ASSERTION_FOR]->(cn);
-- MATCH (a:Assertion {id: $assertionID}), (hd:HistoricDate {id: $historicDateID})
-- CREATE (a)-[:ASSERTION_FOR]->(hd);
-- MATCH (a:Assertion {id: $assertionID}), (cld:Calendar {id: $calendarID})
-- CREATE (a)-[:ASSERTION_FOR]->(cld);
-- MATCH (a:Assertion {id: $assertionID}), (prs:Person {id: $personID})
-- CREATE (a)-[:ASSERTION_FOR]->(prs);
-- MATCH (a:Assertion {id: $assertionID}), (g:Group {id: $groupID})
-- CREATE (a)-[:ASSERTION_FOR]->(g);
-- MATCH (a:Assertion {id: $assertionID}), (m:Media {id: $mediaID})
-- CREATE (a)-[:ASSERTION_FOR]->(m);
-- MATCH (a:Assertion {id: $assertionID}), (pn:PersonName {id: $personNameID})
-- CREATE (a)-[:ASSERTION_FOR]->(pn);

-- Where a source makes an assertion.
-- Transcriptions and transliterations of the extract can be attached through a localized text (with type "extract").
CREATE TABLE CITATION
(
 "ID"           bigint PRIMARY KEY,
 SOURCE_ID      bigint NOT NULL,	-- The source from which this citation is extracted.
 LOCATION       text,					-- The location of the citation inside the source (ex. "page 27, number 8, row 2").
 "EXTRACT"      text NOT NULL,		-- A verbatim copy of any description contained within the source. Text following markdown language. Reference to an entry in a table can be written as `[text](<TABLE_NAME>@<XREF>)`.
 EXTRACT_LOCALE text,					-- Locale as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
 EXTRACT_TYPE   text,					-- Can be 'transcript' (indicates a complete, verbatim copy of the document), 'extract' (a verbatim copy of part of the document), or 'abstract' (a reworded summarization of the document content).
 FOREIGN KEY (SOURCE_ID) REFERENCES SOURCE ( "ID" ) ON DELETE CASCADE
);
-- CREATE (c:Citation {id: $id, location: $location, extract: $extract, extractLocale: $extractLocale, extractType: $extractType});
-- CREATE CONSTRAINT ON (c:Citation) ASSERT exists(c.extract);
-- MATCH (c:Citation {id: $citationID}), (s:Source {id: $sourceID})
-- CREATE (c)-[:CITED_IN]->(s);

-- https://www.evidenceexplained.com/content/sample-quickcheck-models
CREATE TABLE "SOURCE"
(
 "ID"          bigint PRIMARY KEY,
 REPOSITORY_ID bigint NOT NULL,			-- The repository from which this source is contained.
 IDENTIFIER    text NOT NULL UNIQUE,	-- The title of the source (must be unique, ex. "1880 US Census").
 "TYPE"        text,							-- ex. "newspaper", "technical journal", "magazine", "genealogy newsletter", "blog", "baptism record", "birth certificate", "birth register", "book", "grave marker", "census", "death certificate", "yearbook", "directory (organization)", "directory (telephone)", "deed", "land patent", "patent (invention)", "diary", "email message", "interview", "personal knowledge", "family story", "audio record", "video record", "letter/postcard", "probate record", "will", "legal proceedings record", "manuscript", "map", "marriage certificate", "marriage license", "marriage register", "marriage record", "naturalization", "obituary", "pension file", "photograph", "painting/drawing", "passenger list", "tax roll", "death index", "birth index", "town record", "web page", "military record", "draft registration", "enlistment record", "muster roll", "burial record", "cemetery record", "death notice", "marriage index", "alumni publication", "passport", "passport application", "identification card", "immigration record", "border crossing record", "funeral home record", "article", "newsletter", "brochure", "pamphlet", "poster", "jewelry", "advertisement", "cemetery", "prison record", "arrest record".
 AUTHOR        text,							-- The person, agency, or entity who created the record. For a published work, this could be the author, compiler, transcriber, abstractor, or editor. For an unpublished source, this may be an individual, a government agency, church organization, or private organization, etc.
 PLACE_ID      bigint,						-- The place this source was created.
 DATE_ID       bigint,						-- The date this source was created.
 LOCATION      text,							-- Specific location within the information referenced. The data in this field should be in the form of a label and value pair (eg. 'Film: 1234567, Frame: 344, Line: 28').
 FOREIGN KEY (REPOSITORY_ID) REFERENCES REPOSITORY ( "ID" ) ON DELETE CASCADE,
 FOREIGN KEY (PLACE_ID) REFERENCES PLACE ( "ID" ) ON DELETE SET NULL,
 FOREIGN KEY (DATE_ID) REFERENCES HISTORIC_DATE ( "ID" ) ON DELETE SET NULL
);
-- CREATE (s:Source {id: $id, identifier: $identifier, type: $type, author: $author, location: $location});
-- CREATE CONSTRAINT ON (s:Source) ASSERT exists(s.identifier);
-- CREATE CONSTRAINT ON (s:Source) ASSERT s.identifier IS UNIQUE;
-- MATCH (s:Source {id: $sourceID}), (r:Repository {id: $repositoryID})
-- CREATE (s)-[:SOURCED_IN]->(r);
-- MATCH (s:Source {id: $sourceID}), (p:Place {id: $placeID})
-- CREATE (s)-[:CREATED_IN]->(r);
-- MATCH (s:Source {id: $sourceID}), (hd:HistoricDate {id: $historicDateID})
-- CREATE (s)-[:CREATED_WHEN]->(hd);

-- A representation of where a source or set of sources is located
CREATE TABLE REPOSITORY
(
 "ID"       bigint PRIMARY KEY,
 IDENTIFIER text NOT NULL UNIQUE,	-- Repository identifier (must be unique, ex. "familysearch.org", or "University College London").
 "TYPE"     text,							-- Repository type (ex. "public library", "college library", "national library", "prison library", "national archives", "website", "personal collection", "cemetery/mausoleum", "museum", "state library", "religious library", "genealogy society collection", "government agency", "funeral home").
 PERSON_ID  bigint,						-- An xref ID of the person, if present in the tree and is the repository of a source.
 PLACE_ID   bigint,						-- The place this repository is.
 FOREIGN KEY (PERSON_ID) REFERENCES PERSON ( "ID" ) ON DELETE SET NULL,
 FOREIGN KEY (PLACE_ID) REFERENCES PLACE ( "ID" ) ON DELETE SET NULL
);
-- CREATE (r:Repository {id: $id, identifier: $identifier, type: $type});
-- CREATE CONSTRAINT ON (r:Repository) ASSERT exists(r.identifier);
-- CREATE CONSTRAINT ON (r:Repository) ASSERT r.identifier IS UNIQUE;
-- MATCH (prs:Person {id: $personID}), (r:Repository {id: $repositoryID})
-- CREATE (prs)-[:OWNER_OF]->(r);
-- MATCH (r:Repository {id: $repositoryID}), (p:Place {id: $placeID})
-- CREATE (r)-[:LOCATED_IN]->(p);


-- Date

-- https://en.wikipedia.org/wiki/ISO_8601
CREATE TABLE HISTORIC_DATE
(
 "ID"                 bigint PRIMARY KEY,
 "DATE"               text NOT NULL,	-- The date as an ISO 8601 "DD MMM YYYY" date (ex. a "gregorian", "julian", "french republican", "venetan", "hebrew", "muslim", "chinese", "indian", "buddhist", "coptic", "soviet eternal", "ethiopian", "mayan", or whatever converted into ISO 8601).
 DATE_ORIGINAL        text,				-- The date as written into a document.
 CALENDAR_ORIGINAL_ID bigint,				-- An xref ID of a calendar type for the original date.
 CERTAINTY            text,				-- A status code that allows passing on the users opinion of whether the date is correct (ex. "impossible", "unlikely", "possible", "almost certain", "certain").
 CREDIBILITY          text,				-- A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence ("unreliable/estimated data", "questionable reliability of evidence", "secondary evidence, data officially recorded sometime after assertion", "direct and primary evidence used, or by dominance of the evidence").
 FOREIGN KEY (CALENDAR_ORIGINAL_ID) REFERENCES CALENDAR ( "ID" ) ON DELETE RESTRICT
);
-- CREATE (hd:HistoricDate {id: $id, date: $date, dateOriginal: $dateOriginal, certainty: $certainty, credibility: $credibility});
-- CREATE CONSTRAINT ON (hd:HistoricDate) ASSERT exists(hd.date);
-- MATCH (hd:HistoricDate {id: $historicDateID}), (cld:Calendar {id: $calendarOriginalID})
-- CREATE (hd)-[:REPRESENTED_WITHIN]->(cld);

-- https://en.wikipedia.org/wiki/List_of_calendars
CREATE TABLE CALENDAR
(
 "ID"   bigint PRIMARY KEY,
 "TYPE" text NOT NULL UNIQUE	-- A calendar type (must be unique, ex. "gregorian", "julian", "venetan", "french republican", "hebrew", "muslim", "chinese", "indian", "buddhist", "coptic", "soviet eternal", "ethiopian", "mayan").
);
-- CREATE (cld:Calendar {id: $id, type: $type});
-- CREATE CONSTRAINT ON (cld:Calendar) ASSERT exists(cld.type);
-- CREATE CONSTRAINT ON (cld:Calendar) ASSERT cld.type IS UNIQUE;


-- Place

-- Transcriptions and transliterations of the name can be attached through a localized text (with type "name").
-- Additional media can be attached.
CREATE TABLE PLACE
(
 "ID"                   bigint PRIMARY KEY,
 IDENTIFIER             text NOT NULL UNIQUE,	-- An identifier for the place (must be unique).
 NAME                   text NOT NULL,				-- A verbatim copy of the name written in the original language.
 LOCALE                 text,							-- Locale of the name as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
 "TYPE"                 text,							-- The level of the place (ex. "nation", "province", "state", "county", "city", "township", "parish", "island", "archipelago", "continent", "unincorporated town", "settlement", "village", "address").
 COORDINATE             text,							-- Ex. a latitude and longitude pair, or X and Y coordinates.
 COORDINATE_SYSTEM      text,							-- The coordinate system (ex. "WGS84", "UTM").
 COORDINATE_CREDIBILITY text,							-- A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence ("unreliable/estimated data", "questionable reliability of evidence", "secondary evidence, data officially recorded sometime after assertion", "direct and primary evidence used, or by dominance of the evidence").
 PHOTO_ID               bigint,						-- The primary photo for this place.
 PHOTO_CROP             text,							-- Top-left coordinate and width-height length of the enclosing box inside an photo.
 FOREIGN KEY (PHOTO_ID) REFERENCES MEDIA ( "ID" ) ON DELETE SET NULL
);
-- CREATE (p:Place {id: $id, identifier: $identifier, name: $name, locale: $locale, type: $type, coordinate: $coordinate, coordinateSystem: $coordinateSystem, coordinateCredibility: $coordinateCredibility, photoCrop: $photoCrop});
-- CREATE CONSTRAINT ON (p:Place) ASSERT exists(p.identifier);
-- CREATE CONSTRAINT ON (p:Place) ASSERT exists(p.name);
-- CREATE CONSTRAINT ON (p:Place) ASSERT p.identifier IS UNIQUE;
-- MATCH (p:Place {id: $placeID}), (m:Media {id: $photoID})
-- CREATE (p)-[:DEPICTED_WITH]->(m);


--- Localized text - Note

CREATE TABLE LOCALIZED_TEXT
(
 "ID"               bigint PRIMARY KEY,
 "TEXT"             text NOT NULL,	-- Text
 LOCALE             text,				-- The locale identifier for the record (as defined by IETF BCP 47 here https://tools.ietf.org/html/bcp47).
 "TYPE"             text,				-- Can be "original", "transliteration", or "translation".
 TRANSCRIPTION      text,				-- Indicates the system used in transcript the text to the romanized variation (ex. "IPA", "Wade-Giles", "hanyu pinyin", "wāpuro rōmaji", "kana", "hangul").
 TRANSCRIPTION_TYPE text				-- Type of transcription (usually "romanized", but it can be "anglicized", "cyrillized", "francized", "gairaigized", "latinized", etc).
);
-- CREATE (lt:LocalizedText {id: $id, text: $text, locale: $locale, type: $type, transcription: $transcription, transcriptionType: $transcriptionType});
-- CREATE CONSTRAINT ON (lt:LocalizedText) ASSERT exists(lt.text);

CREATE TABLE LOCALIZED_TEXT_JUNCTION
(
 "ID"              bigint PRIMARY KEY,
 LOCALIZED_TEXT_ID bigint NOT NULL,
 JUNCTION_ID       bigint NOT NULL,	-- The ID of the referenced record in the table (tables can be "citation", "place").
 REFERENCE_TYPE    text NOT NULL,	-- The column name this record is attached to (ex. "extract", "name").
 FOREIGN KEY (LOCALIZED_TEXT_ID) REFERENCES LOCALIZED_TEXT ( "ID" ) ON DELETE CASCADE,
 FOREIGN KEY (JUNCTION_ID) REFERENCES JUNCTION ( "ID" ) ON DELETE CASCADE
);
-- MATCH (lt:LocalizedText {id: $localizedTextID}), (c:Citation {id: $citationID})
-- CREATE (lt)-[:NAME_FOR {referenceType: "extract"}]->(c);
-- CREATE CONSTRAINT ON (lt)-[rel:NAME_FOR]->(c) ASSERT exists(rel.referenceType);
-- MATCH (lt:LocalizedText {id: $localizedTextID}), (p:Place {id: $placeID})
-- CREATE (lt)-[:NAME_FOR {referenceType: "name"}]->(p);
-- CREATE CONSTRAINT ON (lt)-[rel:NAME_FOR]->(p) ASSERT exists(rel.referenceType);

CREATE TABLE NOTE
(
 "ID"            bigint PRIMARY KEY,
 NOTE            text NOT NULL,		-- Text following markdown language. Reference to an entry in a table can be written as `[text](<TABLE_NAME>@<XREF>)`.
 LOCALE          text,					-- Locale as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
 JUNCTION_ID     bigint NOT NULL,	-- The ID of the referenced record in the table (tables can be "assertion", "citation", "source", "cultural norm", "historic date", "calendar", "event", "repository", "place", "person name", "person", "group", "research status", "media").
 FOREIGN KEY (JUNCTION_ID) REFERENCES JUNCTION ( "ID" ) ON DELETE CASCADE
);
-- CREATE (n:Note {id: $id, note: $note, locale: $locale});
-- CREATE CONSTRAINT ON (n:Note) ASSERT exists(n.note);
-- MATCH (n:Note {id: $noteID}), (a:Assertion {id: $assertionID})
-- CREATE (n)-[:NOTE_FOR]->(a);
-- MATCH (n:Note {id: $noteID}), (c:Citation {id: $citationID})
-- CREATE (n)-[:NOTE_FOR]->(c);
-- MATCH (n:Note {id: $noteID}), (s:Source {id: $sourceID})
-- CREATE (n)-[:NOTE_FOR]->(s);
-- MATCH (n:Note {id: $noteID}), (cn:CulturalNorm {id: $culturalNormID})
-- CREATE (n)-[:NOTE_FOR]->(cn);
-- MATCH (n:Note {id: $noteID}), (hd:HistoricDate {id: $historicDateID})
-- CREATE (n)-[:NOTE_FOR]->(hd);
-- MATCH (n:Note {id: $noteID}), (cld:Calendar {id: $calendarID})
-- CREATE (n)-[:NOTE_FOR]->(cld);
-- MATCH (n:Note {id: $noteID}), (e:Event {id: $eventID})
-- CREATE (n)-[:NOTE_FOR]->(e);
-- MATCH (n:Note {id: $noteID}), (r:Repository {id: $repositoryID})
-- CREATE (n)-[:NOTE_FOR]->(r);
-- MATCH (n:Note {id: $noteID}), (p:Place {id: $placeID})
-- CREATE (n)-[:NOTE_FOR]->(p);
-- MATCH (n:Note {id: $noteID}), (pn:PersonName {id: $personNameID})
-- CREATE (n)-[:NOTE_FOR]->(pn);
-- MATCH (n:Note {id: $noteID}), (p:Person {id: $personID})
-- CREATE (n)-[:NOTE_FOR]->(p);
-- MATCH (n:Note {id: $noteID}), (g:Group {id: $groupID})
-- CREATE (n)-[:NOTE_FOR]->(g);
-- MATCH (n:Note {id: $noteID}), (rs:ResearchStatus {id: $researchStatusID})
-- CREATE (n)-[:NOTE_FOR]->(rs);
-- MATCH (n:Note {id: $noteID}), (m:Media {id: $mediaID})
-- CREATE (n)-[:NOTE_FOR]->(m);


-- Media

CREATE TABLE MEDIA
(
 "ID"             bigint PRIMARY KEY,
 IDENTIFIER       text NOT NULL UNIQUE,	-- An identifier for the media (must be unique, ex. a complete local or remote file reference (following RFC 1736 specifications) to the auxiliary data).
 TITLE            text,							-- The name of the media.
 PAYLOAD          blob,							-- The media payload.
 "TYPE"           text,							-- (ex. "photo", "audio", "video", "home movie", "newsreel", "microfilm", "microfiche", "cd-rom")
 PHOTO_PROJECTION text,							-- The projection/mapping/coordinate system of an photo. Known values include "spherical UV", "cylindrical equirectangular horizontal"/"cylindrical equirectangular vertical" (equirectangular photo).
 DATE_ID          bigint,						-- The date this media was first recorded.
 FOREIGN KEY (DATE_ID) REFERENCES HISTORIC_DATE ( "ID" ) ON DELETE SET NULL
);
-- CREATE (m:Media {id: $id, identifier: $identifier, title: $title, payload: $payload, type: $type, photoProjection: $photoProjection});
-- CREATE CONSTRAINT ON (m:Media) ASSERT exists(m.identifier);
-- CREATE CONSTRAINT ON (m:Media) ASSERT m.identifier IS UNIQUE;
-- MATCH (m:Media {id: $mediaID}), (hd:HistoricDate {id: $historicDateID})
-- CREATE (m)-[:RECORDED_ON]->(hd);

CREATE TABLE MEDIA_JUNCTION
(
 "ID"            bigint PRIMARY KEY,
 MEDIA_ID        bigint NOT NULL,
 JUNCTION_ID     bigint NOT NULL,	-- The ID of the referenced record in the table (tables can be "cultural norm", "event", "repository", "source", "citation", "assertion", "place", "note", "person", "person name", "group", "research status").
 PHOTO_CROP      text,					-- Top-left coordinate and width-height length of the enclosing box inside an photo.
 FOREIGN KEY (MEDIA_ID) REFERENCES MEDIA ( "ID" ) ON DELETE CASCADE,
 FOREIGN KEY (JUNCTION_ID) REFERENCES JUNCTION ( "ID" ) ON DELETE CASCADE
);
-- MATCH (m:Media {id: $mediaID}), (cn:CulturalNorm {id: $culturalNormID})
-- CREATE (m)-[:MEDIA_FOR]->(cn);
-- MATCH (m:Media {id: $mediaID}), (e:Event {id: $eventID})
-- CREATE (m)-[:MEDIA_FOR]->(e);
-- MATCH (m:Media {id: $mediaID}), (r:Repository {id: $repositoryID})
-- CREATE (m)-[:MEDIA_FOR]->(r);
-- MATCH (m:Media {id: $mediaID}), (s:Source {id: $sourceID})
-- CREATE (m)-[:MEDIA_FOR]->(s);
-- MATCH (m:Media {id: $mediaID}), (c:Citation {id: $citationID})
-- CREATE (m)-[:MEDIA_FOR]->(c);
-- MATCH (m:Media {id: $mediaID}), (a:Assertion {id: $assertionID})
-- CREATE (m)-[:MEDIA_FOR]->(a);
-- MATCH (m:Media {id: $mediaID}), (p:Place {id: $placeID})
-- CREATE (m)-[:MEDIA_FOR]->(p);
-- MATCH (m:Media {id: $mediaID}), (n:Note {id: $noteID})
-- CREATE (m)-[:MEDIA_FOR]->(n);
-- MATCH (m:Media {id: $mediaID}), (p:Person {id: $personID})
-- CREATE (m)-[:MEDIA_FOR]->(p);
-- MATCH (m:Media {id: $mediaID}), (pn:PersonName {id: $personNameID})
-- CREATE (m)-[:MEDIA_FOR]->(pn);
-- MATCH (m:Media {id: $mediaID}), (g:Group {id: $groupID})
-- CREATE (m)-[:MEDIA_FOR]->(g);
-- MATCH (m:Media {id: $mediaID}), (rs:ResearchStatus {id: $researchStatusID})
-- CREATE (m)-[:MEDIA_FOR]->(rs);


-- Person

-- Name can be attached through a person name.
CREATE TABLE PERSON
(
 "ID"       bigint PRIMARY KEY,
 PHOTO_ID   bigint,	-- The primary photo for this person.
 PHOTO_CROP text,		-- Top-left coordinate and width-height length of the enclosing box inside an photo.
 FOREIGN KEY (PHOTO_ID) REFERENCES MEDIA ( "ID" ) ON DELETE SET NULL
);
-- CREATE (prs:Person {id: $id, photoCrop: $photoCrop});
-- MATCH (m:Media {id: $photoID}), (prs:Person {id: $personID})
-- CREATE (m)-[:PHOTO_FOR]->(prs);

-- Transcriptions and transliterations of the name can be attached through a localized person name.
CREATE TABLE PERSON_NAME
(
 "ID"          bigint PRIMARY KEY,
 PERSON_ID     bigint NOT NULL,
 PERSONAL_NAME text,	-- A verbatim copy of the (primary, that is the proper name) name written in the original language.
 FAMILY_NAME   text,	-- A verbatim copy of the (secondary, that is everything that is not a proper name, like a surname) name written in the original language.
 LOCALE        text,	-- Locale of the name as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
 "TYPE"        text,	-- (ex. "birth name" (name given on birth certificate), "also known as" (an unofficial pseudonym, also known as, alias, etc), "nickname" (a familiar name), "family nickname", "pseudonym", "legal" (legally changed name), "adoptive name" (name assumed upon adoption), "stage name", "marriage name" (name assumed at marriage), "call name", "official name", "anglicized name", "religious order name", "pen name", "name at work", "immigrant" (name assumed at the time of immigration) -- see https://github.com/FamilySearch/gedcomx/blob/master/specifications/name-part-qualifiers-specification.md)
 FOREIGN KEY (PERSON_ID) REFERENCES PERSON ( "ID" ) ON DELETE CASCADE
);
-- CREATE (pn:PersonName {id: $id, personalName: $personalName, familyName: $familyName, locale: $locale, type: $type});
-- MATCH (pn:PersonName {id: $personNameID}), (prs:Person {id: $personID})
-- CREATE (pn)-[:NAME_FOR]->(prs);

CREATE TABLE LOCALIZED_PERSON_NAME
(
 "ID"               bigint PRIMARY KEY,
 PERSONAL_NAME      text,					-- A localized (primary, that is the proper name) name.
 FAMILY_NAME        text,					-- A localized (seconday, that is everything that is not a proper name, like a surname) name.
 LOCALE             text,					-- The locale identifier for the record (as defined by IETF BCP 47 here https://tools.ietf.org/html/bcp47).
 "TYPE"             text,					-- Can be "original", "transliteration", or "translation".
 TRANSCRIPTION      text,					-- Indicates the system used in transcript the text to the romanized variation (ex. "IPA", "Wade-Giles", "hanyu pinyin", "wāpuro rōmaji", "kana", "hangul").
 TRANSCRIPTION_TYPE text,					-- Type of transcription (usually "romanized", but it can be "anglicized", "cyrillized", "francized", "gairaigized", "latinized", etc).
 PERSON_NAME_ID     bigint NOT NULL,	-- The ID of the referenced record in the table.
 FOREIGN KEY (PERSON_NAME_ID) REFERENCES PERSON_NAME ( "ID" ) ON DELETE CASCADE
);
-- CREATE (lpn:LocalizedPersonName {id: $id, personalName: $personalName, familyName: $familyName, locale: $locale, type: $type, transcription: $transcription, transcriptionType: $transcriptionType});
-- MATCH (lpn:LocalizedPersonName {id: $personNameID}), (pn:PersonName {id: $personNameID})
-- CREATE (lpn)-[:TRANSCRIPTION_FOR]->(pn);


-- Group

-- A group can be of genealogical, historical, or general interest. Examples of groups that might be useful in genealogy research are households and neighborhoods as found in a census.
CREATE TABLE "GROUP"
(
 "ID"       bigint PRIMARY KEY,
 "TYPE"     text,		-- The type of the group (ex. "family", "neighborhood", "fraternity", "ladies club", "literary society").
 PHOTO_ID   bigint,	-- The primary photo for this group.
 PHOTO_CROP text,		-- Top-left coordinate and width-height length of the enclosing box inside an photo.
 FOREIGN KEY (PHOTO_ID) REFERENCES MEDIA ( "ID" ) ON DELETE SET NULL
);
-- CREATE (g:Group {id: $id, type: $type, photoCrop: $photoCrop});
-- MATCH (m:Media {id: $photoID}), (g:Group {id: $groupID})
-- CREATE (m)-[:PHOTO_FOR]->(g);

CREATE TABLE GROUP_JUNCTION
(
 "ID"            bigint PRIMARY KEY,
 GROUP_ID        bigint NOT NULL,
 JUNCTION_ID     bigint NOT NULL,	-- The ID of the referenced record in the table (tables can be "person", "group", "place").
 ROLE            text,					-- What role the referenced entity played in the group that is being cited in this context (ex. "partner", "child", "adoptee", "president", "member", "resident" (in a neighborhood), "head of household", "tribal leader").
 CERTAINTY       text,					-- A status code that allows passing on the users opinion of whether the group exists (ex. "impossible", "unlikely", "possible", "almost certain", "certain").
 CREDIBILITY     text,					-- A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence ("unreliable/estimated data", "questionable reliability of evidence", "secondary evidence, data officially recorded sometime after assertion", "direct and primary evidence used, or by dominance of the evidence").
 FOREIGN KEY (GROUP_ID) REFERENCES "GROUP" ( "ID" ) ON DELETE CASCADE,
 FOREIGN KEY (JUNCTION_ID) REFERENCES JUNCTION ( "ID" ) ON DELETE CASCADE
);
-- MATCH (g:Group {id: $groupID}), (prs:Person {id: $personID})
-- CREATE (g)-[:GROUP_FOR {role: $role, certainty: $certainty, credibility: $credibility}]->(prs);
-- MATCH (g1:Group {id: $groupID}), (g2:Group {id: $groupID})
-- CREATE (g1)-[:GROUP_FOR {role: $role, certainty: $certainty, credibility: $credibility}]->(g2);
-- MATCH (g:Group {id: $groupID}), (p:Place {id: $placeID})
-- CREATE (g)-[:GROUP_FOR {role: $role, certainty: $certainty, credibility: $credibility}]->(p);


-- Event

/*
1. Assertion can be made about "place", "cultural norm", "historic date", "calendar", "person", "group", "media", "person name".
2. A conclusion is an assertion that is substantiated, different from a bare assertion that is not substantiated.
3. An event is a collection of conclusions/bare assertions about something (a description) happened somewhere ("place") at a certain time ("historic date") to someone ("person", "group") or something ("place", "cultural norm", "calendar", "media", "person name").
*/
CREATE TABLE EVENT
(
 "ID"            bigint PRIMARY KEY,
 "TYPE_ID"       bigint NOT NULL,
 DESCRIPTION     text,				   -- The description of the event.
 PLACE_ID        bigint,				-- The place this event happened.
 DATE_ID         bigint,				-- The date this event has happened.
 JUNCTION_ID     bigint NOT NULL,	-- The ID of the referenced record in the table (tables can be "person", "group", "place", "cultural norm", "calendar", "media", "person name").
 FOREIGN KEY ("TYPE_ID") REFERENCES EVENT_TYPE ( "ID" ) ON DELETE RESTRICT,
 FOREIGN KEY (PLACE_ID) REFERENCES PLACE ( "ID" ) ON DELETE SET NULL,
 FOREIGN KEY (DATE_ID) REFERENCES HISTORIC_DATE ( "ID" ) ON DELETE SET NULL,
 FOREIGN KEY (JUNCTION_ID) REFERENCES JUNCTION ( "ID" ) ON DELETE CASCADE
);
-- CREATE (e:Event {id: $id, description: $description});
-- MATCH (e:Event {id: $eventID}), (et:EventType {id: $eventTypeID})
-- CREATE (e)-[:OF_TYPE]->(et);
-- MATCH (e:Event {id: $eventID}), (p:Place {id: $placeID})
-- CREATE (e)-[:HAPPENED_IN]->(p);
-- MATCH (e:Event {id: $eventID}), (hd:HistoricDate {id: $historicDateID})
-- CREATE (e)-[:HAPPENED_ON]->(hd);
-- MATCH (e:Event {id: $eventID}), (prs:Person {id: $personID})
-- CREATE (e)-[:FOR]->(prs);
-- MATCH (e:Event {id: $eventID}), (g:Group {id: $groupID})
-- CREATE (e)-[:FOR]->(g);
-- MATCH (e:Event {id: $eventID}), (p:Place {id: $placeID})
-- CREATE (e)-[:FOR]->(p);
-- MATCH (e:Event {id: $eventID}), (cn:CulturalNorm {id: $culturalNormID})
-- CREATE (e)-[:FOR]->(cn);
-- MATCH (e:Event {id: $eventID}), (cld:Calendar {id: $calendarID})
-- CREATE (e)-[:FOR]->(cld);
-- MATCH (e:Event {id: $eventID}), (m:Media {id: $mediaID})
-- CREATE (e)-[:FOR]->(m);
-- MATCH (e:Event {id: $eventID}), (pn:PersonName {id: $personNameID})
-- CREATE (e)-[:FOR]->(pn);

CREATE TABLE EVENT_TYPE
(
 "ID"          bigint PRIMARY KEY,
 SUPER_TYPE_ID bigint NOT NULL,
 "TYPE"        text NOT NULL,	-- (ex. Historical events: "historic fact", "natural disaster", "invention", "patent filing", "patent granted", Personal origins: "birth", "sex", "fosterage", "adoption", "guardianship", Physical description: "physical description", "eye color", "hair color", "height", "weight", "build", "complexion", "gender", "race", "ethnic origin", "marks/scars", "special talent", "disability", Citizenship and migration: "nationality", "emigration", "immigration", "naturalization", "caste", Real estate assets: "residence", "land grant", "land purchase", "land sale", "property", "deed", "escrow", Education: "education", "graduation", "able to read", "able to write", "learning", "enrollment", Work and Career: "employment", "occupation", "career", "retirement", "resignation", Legal Events and Documents: "coroner report", "will", "probate", "legal problem", "name change", "inquest", "jury duty", "draft registration", "pardon", Health problems and habits: "hospitalization", "illness", "tobacco use", "alcohol use", "drug problem", Marriage and family life: "engagement", "betrothal", "cohabitation", "union", "wedding", "marriage", "number of marriages", "marriage bann", "marriage license", "marriage contract", "marriage settlement", "filing for divorce", "divorce", "annulment", "separation", "number of children (total)", "number of children (living)", "marital status", "wedding anniversary", "anniversary celebration", Military: "military induction", "military enlistment", "military rank", "military award", "military promotion", "military service", "military release", "military discharge", "military resignation", "military retirement", "missing in action", Confinement: "imprisonment", "deportation", "internment", Transfers and travel: "travel", Accolades: "honor", "award", "membership", Death and burial: "death", "execution", "autopsy", "funeral", "cremation", "scattering of ashes", "inurnment", "burial", "exhumation", "reburial", Others: "anecdote", "political affiliation", "hobby", "partnership", "celebration of life", "ran away from home", Religious events: "religion", "religious conversion", "bar mitzvah", "bas mitzvah", "baptism", "excommunication", "christening", "confirmation", "ordination", "blessing", "first communion")
 CATEGORY      text,				-- (ex. birth of a person: "birth", death of a person: "death", "execution", union between two persons: "betrothal", "cohabitation", "union", "wedding", "marriage", "marriage bann", "marriage license", "marriage contract", adoption of a person: "adoption", "fosterage")
 FOREIGN KEY (SUPER_TYPE_ID) REFERENCES EVENT_SUPER_TYPE ( "ID" ) ON DELETE RESTRICT
);
-- CREATE (et:EventType {id: $id, type: $type, category: $category});
-- CREATE CONSTRAINT ON (et:EventType) ASSERT exists(et.type);
-- MATCH (et:EventType {id: $eventTypeID}), (est:EventSuperType {id: $eventSuperTypeID})
-- CREATE (et)-[:OF]->(pn);

CREATE TABLE EVENT_SUPER_TYPE
(
 "ID"       bigint PRIMARY KEY,
 SUPER_TYPE text NOT NULL UNIQUE	-- (must be unique, ex. "Historical events", "Personal origins", "Physical description", "Citizenship and migration", "Real estate assets", "Education", "Work and Career", "Legal Events and Documents", "Health problems and habits", "Marriage and family life", "Military", "Confinement", "Transfers and travel", "Accolades", "Death and burial", "Others", "Religious events")
);
-- CREATE (est:EventSuperType {id: $id, superType: $superType});
-- CREATE CONSTRAINT ON (est:EventSuperType) ASSERT exists(est.superType);
-- CREATE CONSTRAINT ON (est:EventSuperType) ASSERT est.superType IS UNIQUE;


-- Cultural norm

-- Genealogical events and individual characteristics at various times and places are influenced by customs, practices, and conditions of their culture. This effects the interpretation of recorded information and the assertions made about a citation.
/*
Ex.
 - per i nomi in latino si deve usare il nominativo (quello che generalmente finisce in *-us* per il maschile e *-a* per il femminile).

https://it.wikisource.org/wiki/Codice_di_Napoleone_il_grande/Libro_I/Titolo_V
https://www.google.com/url?sa=t&source=web&rct=j&opi=89978449&url=https://elearning.unite.it/pluginfile.php/284578/mod_folder/content/0/ZZ%2520-%2520Lezioni%252028-30%2520novembre%25202023/3.%2520L_EREDITA_NAPOLEONICA_lezione.pdf%3Fforcedownload%3D1&ved=2ahUKEwiMj_Lx2-SGAxVM_7sIHc5-ATEQFnoECA8QAw&usg=AOvVaw1VloSAbSzRjtU1QBFag5uC
 - uomini: 23 anni minore, 29 anni maggiore (31 JAN 1807 - 19 FEB 1811) (25, atto rispettoso comunque fino ai 30).
 - donne: 22 anni minore (31 JAN 1807 - 13 MAY 1809) (21, atto rispettoso comunque fino ai 25).
*/
CREATE TABLE CULTURAL_NORM
(
 "ID"          bigint PRIMARY KEY,
 IDENTIFIER    text NOT NULL UNIQUE,	-- An identifier of the rule (must be unique).
 DESCRIPTION   text,							-- The description of the rule.
 PLACE_ID      bigint,						-- The place this rule applies.
 DATE_START_ID bigint,						-- The date this cultural norm went into effect.
 DATE_END_ID   bigint,						-- The date this cultural norm stopped being in effect.
 CERTAINTY     text,							-- A status code that allows passing on the users opinion of whether the rule is true (ex. "impossible", "unlikely", "possible", "almost certain", "certain").
 CREDIBILITY   text,							-- A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence ("unreliable/estimated data", "questionable reliability of evidence", "secondary evidence, data officially recorded sometime after assertion", "direct and primary evidence used, or by dominance of the evidence").
 FOREIGN KEY (PLACE_ID) REFERENCES PLACE ( "ID" ) ON DELETE SET NULL,
 FOREIGN KEY (DATE_START_ID) REFERENCES HISTORIC_DATE ( "ID" ) ON DELETE SET NULL,
 FOREIGN KEY (DATE_END_ID) REFERENCES HISTORIC_DATE ( "ID" ) ON DELETE SET NULL
);
-- CREATE (cn:CulturalNorm {id: $id, identifier: $identifier, description: $description, certainty: $certainty, credibility: $credibility});
-- CREATE CONSTRAINT ON (cn:CulturalNorm) ASSERT exists(cn.identifier);
-- CREATE CONSTRAINT ON (cn:CulturalNorm) ASSERT cn.identifier IS UNIQUE;
-- MATCH (cn:CulturalNorm {id: $culturalNormID}), (p:Place {id: $placeID})
-- CREATE (cn)-[:APPLY_IN]->(p);
-- MATCH (cn:CulturalNorm {id: $culturalNormID}), (hds:HistoricDate {id: $historicDateID})
-- CREATE (cn)-[:START_ON]->(hds);
-- MATCH (cn:CulturalNorm {id: $culturalNormID}), (hde:HistoricDate {id: $historicDateID})
-- CREATE (cn)-[:END_ON]->(hde);

CREATE TABLE CULTURAL_NORM_JUNCTION
(
 "ID"             bigint PRIMARY KEY,
 CULTURAL_NORM_ID bigint NOT NULL,
 JUNCTION_ID      bigint NOT NULL,	-- The ID of the referenced record in the table (tables can be "assertion", "note", "person name", "group").
 CERTAINTY        text,					-- A status code that allows passing on the users opinion of whether the connection to the rule is true (ex. "impossible", "unlikely", "possible", "almost certain", "certain").
 CREDIBILITY      text,					-- A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence ("unreliable/estimated data", "questionable reliability of evidence", "secondary evidence, data officially recorded sometime after assertion", "direct and primary evidence used, or by dominance of the evidence").
 FOREIGN KEY (CULTURAL_NORM_ID) REFERENCES CULTURAL_NORM ( "ID" ) ON DELETE CASCADE,
 FOREIGN KEY (JUNCTION_ID) REFERENCES JUNCTION ( "ID" ) ON DELETE CASCADE
);
-- MATCH (cn:CulturalNorm {id: $culturalNormID}), (a:Assertion {id: $assertionID})
-- CREATE (cn)-[:END_ON {certainty: $certainty, credibility: $credibility}]->(a);
-- MATCH (cn:CulturalNorm {id: $culturalNormID}), (n:Note {id: $noteID})
-- CREATE (cn)-[:END_ON {certainty: $certainty, credibility: $credibility}]->(n);
-- MATCH (cn:CulturalNorm {id: $culturalNormID}), (pn:PersonName {id: $personNameID})
-- CREATE (cn)-[:END_ON {certainty: $certainty, credibility: $credibility}]->(pn);
-- MATCH (cn:CulturalNorm {id: $culturalNormID}), (g:Group {id: $groupID})
-- CREATE (cn)-[:END_ON {certainty: $certainty, credibility: $credibility}]->(g);


-- Other application-related things

-- tables that have a REFERENCE_TABLE/REFERENCE_ID: ASSERTION, NOTE, EVENT, plus all *_JUNCTION
CREATE TABLE JUNCTION
(
 "ID"            bigint PRIMARY KEY,
 REFERENCE_TABLE text NOT NULL,	-- The table name this record is attached to.
 REFERENCE_ID    bigint NOT NULL	-- The ID of the referenced record in the table.
);

CREATE TABLE RESTRICTION
(
 "ID"            bigint PRIMARY KEY,
 RESTRICTION     text NOT NULL,		-- Specifies how the record should be treated. Known values and their meaning are: "confidential" (should not be distributed or exported), "public" (can be freely distributed or exported).
 JUNCTION_ID     bigint NOT NULL,	-- The ID of the referenced record in the table (tables can be "assertion", "citation", "source", "repository", "cultural norm", "historic date", "event", "place", "note", "person name", "person", "group", "media").
 FOREIGN KEY (JUNCTION_ID) REFERENCES JUNCTION ( "ID" ) ON DELETE CASCADE
);
-- CREATE (rst:Restriction {id: $id, restriction: $restriction});
-- CREATE CONSTRAINT ON (rst:Restriction) ASSERT exists(rst.restriction);
-- MATCH (rst:Restriction {id: $restrictionID}), (a:Assertion {id: $assertionID})
-- CREATE (r)-[:FOR]->(a);
-- MATCH (rst:Restriction {id: $restrictionID}), (c:Citation {id: $citationID})
-- CREATE (r)-[:FOR]->(c);
-- MATCH (rst:Restriction {id: $restrictionID}), (s:Source {id: $sourceID})
-- CREATE (r)-[:FOR]->(s);
-- MATCH (rst:Restriction {id: $restrictionID}), (r:Repository {id: $repositoryID})
-- CREATE (rst)-[:FOR]->(r);
-- MATCH (rst:Restriction {id: $restrictionID}), (cn:CulturalNorm {id: $culturalNormID})
-- CREATE (r)-[:FOR]->(cn);
-- MATCH (rst:Restriction {id: $restrictionID}), (hd:HistoricDate {id: $historicDateID})
-- CREATE (r)-[:FOR]->(hd);
-- MATCH (rst:Restriction {id: $restrictionID}), (e:Event {id: $eventID})
-- CREATE (r)-[:FOR]->(e);
-- MATCH (rst:Restriction {id: $restrictionID}), (p:Place {id: $placeID})
-- CREATE (r)-[:FOR]->(p);
-- MATCH (rst:Restriction {id: $restrictionID}), (n:Note {id: $noteID})
-- CREATE (r)-[:FOR]->(n);
-- MATCH (rst:Restriction {id: $restrictionID}), (pn:PersonName {id: $personNameID})
-- CREATE (r)-[:FOR]->(pn);
-- MATCH (rst:Restriction {id: $restrictionID}), (prs:Person {id: $personID})
-- CREATE (r)-[:FOR]->(prs);
-- MATCH (rst:Restriction {id: $restrictionID}), (g:Group {id: $groupID})
-- CREATE (r)-[:FOR]->(g);
-- MATCH (rst:Restriction {id: $restrictionID}), (m:Media {id: $mediaID})
-- CREATE (r)-[:FOR]->(m);

-- Notes can be attached through a note.
CREATE TABLE MODIFICATION
(
 "ID"            bigint PRIMARY KEY,
 JUNCTION_ID     bigint NOT NULL,		-- The ID of the referenced record in the table.
 CREATION_DATE   timestamp NOT NULL,	-- The creation date of a record.
 UPDATE_DATE     timestamp,				-- The changing date of a record.
 FOREIGN KEY (JUNCTION_ID) REFERENCES JUNCTION ( "ID" ) ON DELETE CASCADE
);
-- CREATE (mdf:Modification {id: $id, creationDate: $creationDate, updateDate: $updateDate});
-- CREATE CONSTRAINT ON (mdf:Modification) ASSERT exists(mdf.creationDate);
-- TODO
-- MATCH (mdf:Modification {id: $modificationID}), (?:? {id: $?ID})
-- CREATE (mdf)-[:FOR]->(?);

CREATE TABLE RESEARCH_STATUS
(
 "ID"            bigint PRIMARY KEY,
 JUNCTION_ID     bigint NOT NULL,		-- The ID of the referenced record in the table.
 IDENTIFIER      text NOT NULL UNIQUE,	-- An identifier (must be unique).
 DESCRIPTION     text,						-- The description of the research status. Text following markdown language. Reference to an entry in a table can be written as `[text](<TABLE_NAME>@<XREF>)`.
 STATUS          text,						-- Research status (ex. "open": recorded but not started yet, "active": currently being searched, "ended": all the information has been found).
 PRIORITY        smallint,
 CREATION_DATE   timestamp NOT NULL,		-- The creation date.
 FOREIGN KEY (JUNCTION_ID) REFERENCES JUNCTION ( "ID" ) ON DELETE CASCADE
);
-- CREATE (rs:ResearchStatus {id: $id, identifier: $identifier, description: $description, status: $status, priority: $priority, creationDate: $creationDate});
-- CREATE CONSTRAINT ON (rs:ResearchStatus) ASSERT exists(rs.identifier);
-- CREATE CONSTRAINT ON (rs:ResearchStatus) ASSERT exists(rs.creationDate);
-- CREATE CONSTRAINT ON (rs:ResearchStatus) ASSERT rs.identifier IS UNIQUE;
-- TODO
-- MATCH (rs:ResearchStatus {id: $researchStatusID}), (?:? {id: $?ID})
-- CREATE (rs)-[:FOR]->(?);

CREATE TABLE CONTACT
(
 "ID"      bigint PRIMARY KEY,
 CALLER_ID text NOT NULL,	-- Indicates the name of the person associated with this contact.
 NOTE      text				-- Text following markdown language. Reference to an entry in a table can be written as `[text](<TABLE_NAME>@<XREF>)`. Usually it contains a phone number, languages spoken, an electronic address that can be used for contact such as an email address following RFC 5322 specifications, or a World Wide Web page address following RFC 1736 specifications, or any other type of contact address, or other things.
);
-- CREATE (ctc:Contact {id: $id, callerID: $callerID, note: $note});
-- CREATE CONSTRAINT ON (ctc:Contact) ASSERT exists(ctc.callerID);

CREATE TABLE CONTACT_JUNCTION
(
 "ID"            bigint PRIMARY KEY,
 CONTACT_ID      bigint NOT NULL,
 JUNCTION_ID     bigint NOT NULL,	-- The ID of the referenced record in the table.
 FOREIGN KEY (CONTACT_ID) REFERENCES CONTACT ( "ID" ) ON DELETE CASCADE,
 FOREIGN KEY (JUNCTION_ID) REFERENCES JUNCTION ( "ID" ) ON DELETE CASCADE
);
-- TODO
-- MATCH (ctc:Contact {id: $contactID}), (?:? {id: $?ID})
-- CREATE (ctc)-[:FOR]->(?);

CREATE TABLE PROJECT
(
 "ID"             bigint PRIMARY KEY,
 PROTOCOL_NAME    text NOT NULL,			-- "Family LEgacy Format"
 PROTOCOL_VERSION text NOT NULL,			-- "0.0.10"
 COPYRIGHT        text,						-- A copyright statement.
 NOTE             text,						-- Text following markdown language.
 LOCALE           text,						-- Locale as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
 CREATION_DATE    timestamp NOT NULL,	-- The creation date of the project.
 UPDATE_DATE      timestamp				-- The changing date of the project.
);
-- CREATE (p:Project {id: $id, protocolName: $protocolName, protocolVersion: $protocolVersion, copyright: $copyright, note: $note, locale: $locale, creationDate: $creationDate, updateDate: $updateDate});
-- CREATE CONSTRAINT ON (p:Project) ASSERT exists(p.protocolName);
-- CREATE CONSTRAINT ON (p:Project) ASSERT exists(p.protocolVersion);
-- CREATE CONSTRAINT ON (p:Project) ASSERT exists(p.creationDate);



-- https://wiki.phpgedview.net/en/index.php/Facts_and_Events
-- http://www.gencom.org.nz/GEDCOM_tags.html
-- Life related types are: BIRTH (the exiting of the womb), MIDWIFE, ADOPTION (the creation of a parent-child relationship not associated with birth), CHARACTERISTIC (physical characteristics of a person, in the format `key1: value1, key2: value2`), ANECDOTE, DEATH (the end of life), CHILDREN_COUNT (the reported number of children known to belong to this family, regardless of whether the associated children are represented in the corresponding structure; this is not necessarily the count of children listed in a family structure), MARRIAGES_COUNT, CORONER_REPORT (the act of including an individual in a report by a coroner, a public official, on the investigation into the causes and circumstances of any death which occurred through violence or suddenly with marks of suspicion), BURIAL (the depositing of the body (in whole or in part) of the deceased), CREMATION (the burning of the body (in whole or in part) of the deceased).
-- Family related types are: ENGAGEMENT (the agreement of a couple to enter into a marriage in the future), MARRIAGE_BANN (a public notice of an intent to marry), MARRIAGE_CONTRACT (a formal contractual agreement to marry), MARRIAGE_LICENCE (obtaining a legal license to marry), MARRIAGE_SETTLEMENT (a legal arrangement to modify property rights upon marriage), MARRIAGE (the creation of a family unit (via a legal, religious, customary, common-law, or other form of union)), DIVORCE_FILED (the legal action expressing intent to divorce), DIVORCE_DECREE (an individual's participation in a decree of a court marking the legal separation of a man and woman, totally dissolving the marriage relation), DIVORCE (the ending of a marriage between still-living individuals), ANNULMENT (declaring a marriage to be invalid, as though it had never occurred).
-- Achievements related types are: RESIDENCE, EDUCATION (an educational degree or attainment), GRADUATION (the conclusion of formal education), OCCUPATION (what this person does as a livelihood), RETIREMENT (the cessation of gainful employment, typically because sufficient wealth has been accumulated to no longer necessitate such), MILITARY_AWARD (the act of receiving a medal or honor for military service in a particular campaign or for a particular service), MILITARY_DISCHARGE (a release from serving in the armed forces), MILITARY_INDUCTION (to take into the armed forces), MILITARY_MUSTER_ROLL (the act of including an individual in a list or account of the enlisted persons in a military or naval unit), MILITARY_SERVICE (the act of serving in the armed forces), MILITARY_RANK (the military rank acquired by an individual), MILITARY_RELEASE (the act of releasing from active to inactive military duty), MILITARY_RESIGNATION (the act of resigning from serving in the military; resigning a commission), MILITARY_RETIREMENT (to retire or withdraw from active duty in the armed forces), PRISON (the act of punishment imposed by law or otherwise in the course of administration of justice), PARDON (the act of exempting an individual from the punishment the law inflicts for a crime that person has committed), MEMBERSHIP (the act of becoming a member; joining a group), JURY_DUTY (the act of serving on a jury), MEDICAL, HOSPITALIZATION, ILLNESS (the act of losing good health; sickness; disease), HONOR (to be recognized for an achievement), HOLOCAUST_LIBERATION (the liberation of the Holocaust survivor from place of internment), HOLOCAUST_DEPORTATION (deportation of Holocaust victim/survivor from place of residence), HOLOCAUST_DEPARTURE (the departure of the Holocaust survivor from the place of internment), HOLOCAUST_ARRIVAL (the arrival of a Holocaust victim/survivor to the place of internment) EMANCIPATION (the emancipation of a minor child by its parents, which involves an entire surrender of the right to the care, custody, and earnings of such child as well as the renunciation of parental duties), BANKRUPTCY (the act of taking possession by the trustee of property of the bankrupt).
-- National/government related types are: CASTE (the social, religious, or racial caste tow which an individual belongs), NATIONALITY (a group to which a person is associated, typically by birth, like nation or tribe), EMIGRATION (the departure from the nation or land in which one has nativity or citizenship), IMMIGRATION (the entering of a nation or land in which one does not have nativity or citizenship), NATURALIZATION (the gaining of citizenship in a new nation or land), CENSUS (an inventory of persons or households in a population), SSN (Social Security Number).
-- Possessions and titles related types are: POSSESSION (a list of objects or land owned by the person), TITLE (a title given a person associated with a local or national notion of nobility or royalty), WILL (the creation of a legal document regarding the disposition of a person’s estate upon death), PROBATE (the judicial actions associated with the disposition of the estate of the deceased), DEED (the judicial actions associated with the transferring (conveyancing) the title to a property), GUARDIANSHIP (the act of legally appointing an individual to take care of the affairs of someone who is young or cannot take care of her/himself), ESCROW (an individual's participation in a deed or bond held by a third party until certain conditions are met by other parties), CHANCERY (the resolution through impartial justice between two parties whose claims conflict).
-- Religious and social related types are: RELIGION (the name of a religion with which the event was affiliated).
-- Custom type: <EVENT_TYPE> (a descriptive word or phrase used to further classify the parent event or the attribute tag).
