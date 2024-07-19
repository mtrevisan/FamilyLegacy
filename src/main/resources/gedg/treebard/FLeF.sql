-- https://treebard.com/gedcom.html#relationships
-- https://treebard.proboards.com/search/results?captcha_id=captcha_search&what_all=database+table+description&who_only_made_by=0&display_as=0&search=Search
-- https://app.sqldbm.com/PostgreSQL/DatabaseExplorer/p302632/


-- Assertion - Citation - Source - Repository

-- What the source says at the citation within the source.
CREATE TABLE "ASSERTION"
(
 "ID"            numeric PRIMARY KEY,
 CITATION_ID     numeric NOT NULL,	-- The citation from which this assertion is derived.
 REFERENCE_TABLE text,					-- The table name this record is attached to (ex. "place", "cultural norm", "historic date", "calendar", "person", "group", "media", "person name").
 REFERENCE_ID    numeric,				-- The ID of the referenced record in the table.
 ROLE            text,					-- What role the cited entity played in the event that is being cited in this context (ex. "child", "father", "mother", "partner", "midwife", "bridesmaid", "best man", "parent", "prisoner", "religious officer", "justice of the peace", "supervisor", "emproyer", "employee", "witness", "assistant", "roommate", "landlady", "landlord", "foster parent", "makeup artist", "financier", "florist", "usher", "photographer", "bartender", "bodyguard", "adoptive parent", "hairdresser", "chauffeur", "treasurer", "trainer", "secretary", "navigator", "pallbreare", "neighbor", "maid", "pilot", "undertaker", "mining partner", "legal guardian", "interior decorator", "executioner", "driver", "host", "hostess", "farm hand", "ranch hand", "junior partner", "butler", "boarder", "chef", "patent attorney").
 CERTAINTY       text,					-- A status code that allows passing on the users opinion of whether the assertion cause has really caused the assertion (ex. "impossible", "unlikely", "possible", "almost certain", "certain").
 CREDIBILITY     text,					-- A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence ("unreliable/estimated data", "questionable reliability of evidence", "secondary evidence, data officially recorded sometime after assertion", "direct and primary evidence used, or by dominance of the evidence").
 FOREIGN KEY (CITATION_ID) REFERENCES CITATION ( "ID" ) ON DELETE CASCADE
);

-- Where a source makes an assertion.
-- Transcriptions and transliterations of the extract can be attached through a localized text (with type "extract").
CREATE TABLE CITATION
(
 "ID"           numeric PRIMARY KEY,
 SOURCE_ID      numeric NOT NULL,	-- The source from which this citation is extracted.
 LOCATION       text,					-- The location of the citation inside the source (ex. "page 27, number 8, row 2").
 "EXTRACT"      text NOT NULL,		-- A verbatim copy of any description contained within the source. Text following markdown language. Reference to an entry in a table can be written as `[text](<TABLE_NAME>@<XREF>)`.
 EXTRACT_LOCALE text,					-- Locale as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
 EXTRACT_TYPE   text,					-- Can be 'transcript' (indicates a complete, verbatim copy of the document), 'extract' (a verbatim copy of part of the document), or 'abstract' (a reworded summarization of the document content).
 FOREIGN KEY (SOURCE_ID) REFERENCES SOURCE ( "ID" ) ON DELETE CASCADE
);

-- https://www.evidenceexplained.com/content/sample-quickcheck-models
CREATE TABLE "SOURCE"
(
 "ID"          numeric PRIMARY KEY,
 REPOSITORY_ID numeric NOT NULL UNIQUE,	-- The repository from which this source is contained.
 IDENTIFIER    text NOT NULL UNIQUE,	-- The title of the source (must be unique, ex. "1880 US Census").
 "TYPE"        text,							-- ex. "newspaper", "technical journal", "magazine", "genealogy newsletter", "blog", "baptism record", "birth certificate", "birth register", "book", "grave marker", "census", "death certificate", "yearbook", "directory (organization)", "directory (telephone)", "deed", "land patent", "patent (invention)", "diary", "email message", "interview", "personal knowledge", "family story", "audio record", "video record", "letter/postcard", "probate record", "will", "legal proceedings record", "manuscript", "map", "marriage certificate", "marriage license", "marriage register", "marriage record", "naturalization", "obituary", "pension file", "photograph", "painting/drawing", "passenger list", "tax roll", "death index", "birth index", "town record", "web page", "military record", "draft registration", "enlistment record", "muster roll", "burial record", "cemetery record", "death notice", "marriage index", "alumni publication", "passport", "passport application", "identification card", "immigration record", "border crossing record", "funeral home record", "article", "newsletter", "brochure", "pamphlet", "poster", "jewelry", "advertisement", "cemetery", "prison record", "arrest record".
 AUTHOR        text,								-- The person, agency, or entity who created the record. For a published work, this could be the author, compiler, transcriber, abstractor, or editor. For an unpublished source, this may be an individual, a government agency, church organization, or private organization, etc.
 PLACE_ID      numeric,							-- The place this source was created.
 DATE_ID       numeric,							-- The date this source was created.
 LOCATION      text,								-- Specific location within the information referenced. The data in this field should be in the form of a label and value pair (eg. 'Film: 1234567, Frame: 344, Line: 28').
 FOREIGN KEY (REPOSITORY_ID) REFERENCES REPOSITORY ( "ID" ) ON DELETE CASCADE,
 FOREIGN KEY (PLACE_ID) REFERENCES PLACE ( "ID" ) ON DELETE SET NULL,
 FOREIGN KEY (DATE_ID) REFERENCES HISTORIC_DATE ( "ID" ) ON DELETE SET NULL
);

-- A representation of where a source or set of sources is located
CREATE TABLE REPOSITORY
(
 "ID"       numeric PRIMARY KEY,
 IDENTIFIER text NOT NULL UNIQUE,	-- Repository identifier (must be unique, ex. "familysearch.org", or "University College London").
 "TYPE"     text,							-- Repository type (ex. "public library", "college library", "national library", "prison library", "national archives", "website", "personal collection", "cemetery/mausoleum", "museum", "state library", "religious library", "genealogy society collection", "government agency", "funeral home").
 PERSON_ID  numeric,						-- An xref ID of the person, if present in the tree and is the repository of a source.
 PLACE_ID   numeric,						-- The place this repository is.
 FOREIGN KEY (PERSON_ID) REFERENCES PERSON ( "ID" ) ON DELETE SET NULL,
 FOREIGN KEY (PLACE_ID) REFERENCES PLACE ( "ID" ) ON DELETE SET NULL
);


-- Date

CREATE TABLE HISTORIC_DATE
(
 "ID"                 numeric PRIMARY KEY,
 "DATE"               text NOT NULL,	-- The date.
 CALENDAR_ID          numeric,			-- An xref ID of a calendar type.
 DATE_ORIGINAL        text,				-- The date as written into a document.
 CALENDAR_ORIGINAL_ID numeric,			-- An xref ID of a calendar type for the original date.
 CERTAINTY            text,				-- A status code that allows passing on the users opinion of whether the date is correct (ex. "impossible", "unlikely", "possible", "almost certain", "certain").
 CREDIBILITY          text,				-- A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence ("unreliable/estimated data", "questionable reliability of evidence", "secondary evidence, data officially recorded sometime after assertion", "direct and primary evidence used, or by dominance of the evidence").
 FOREIGN KEY (CALENDAR_ID) REFERENCES CALENDAR ( "ID" ) ON DELETE SET NULL,
 FOREIGN KEY (CALENDAR_ORIGINAL_ID) REFERENCES CALENDAR ( "ID" ) ON DELETE SET NULL
);

CREATE TABLE CALENDAR
(
 "ID"   numeric PRIMARY KEY,
 "TYPE" text NOT NULL	-- A calendar type (ex. "gregorian", "julian", "islamic", "hebrew", "chinese", "indian", "buddhist", "french republican", "coptic", "soviet eternal", "ethiopian", "mayan", "venetan").
);


-- Place

-- Transcriptions and transliterations of the name can be attached through a localized text (with type "name").
-- Additional media can be attached.
CREATE TABLE PLACE
(
 "ID"                   numeric PRIMARY KEY,
 IDENTIFIER             text NOT NULL UNIQUE,	-- An identifier for the place (must be unique).
 NAME                   text NOT NULL,				-- A verbatim copy of the name written in the original language.
 NAME_LOCALE            text,							-- Locale of the name as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
 "TYPE"                 text,							-- The level of the place (ex. "nation", "province", "state", "county", "city", "township", "parish", "island", "archipelago", "continent", "unincorporated town", "settlement", "village", "address").
 COORDINATE             text,							-- Ex. a latitude and longitude pair, or X and Y coordinates.
 COORDINATE_SYSTEM      text,							-- The coordinate system (ex. "WGS84", "UTM").
 COORDINATE_CREDIBILITY text,							-- A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence ("unreliable/estimated data", "questionable reliability of evidence", "secondary evidence, data officially recorded sometime after assertion", "direct and primary evidence used, or by dominance of the evidence").
 PHOTO_ID               numeric,						-- The primary photo for this place.
 PHOTO_CROP             text,							-- Top-left coordinate and width-height length of the enclosing box inside an photo.
 FOREIGN KEY (PHOTO_ID) REFERENCES MEDIA ( "ID" ) ON DELETE SET NULL
);


--- Localized text - Note

CREATE TABLE LOCALIZED_TEXT
(
 "ID"               numeric PRIMARY KEY,
 "TEXT"             text NOT NULL,	-- Text
 LOCALE             text,				-- The locale identifier for the record (as defined by IETF BCP 47 here https://tools.ietf.org/html/bcp47).
 "TYPE"             text,				-- Can be "original", "transliteration", or "translation".
 TRANSCRIPTION      text,				-- Indicates the system used in transcript the text to the romanized variation (ex. "IPA", "Wade-Giles", "hanyu pinyin", "wāpuro rōmaji", "kana", "hangul").
 TRANSCRIPTION_TYPE text				-- Type of transcription (usually "romanized", but it can be "anglicized", "cyrillized", "francized", "gairaigized", "latinized", etc).
);

CREATE TABLE LOCALIZED_TEXT_JUNCTION
(
 "ID"              numeric PRIMARY KEY,
 LOCALIZED_TEXT_ID numeric NOT NULL,
 REFERENCE_TABLE   text NOT NULL,		-- The table name this record is attached to (ex. "citation", "person name", "place").
 REFERENCE_ID      numeric NOT NULL,	-- The ID of the referenced record in the table.
 REFERENCE_TYPE    text NOT NULL,		-- The column name this record is attached to (ex. "extract", "name").
 FOREIGN KEY (LOCALIZED_TEXT_ID) REFERENCES LOCALIZED_TEXT ( "ID" ) ON DELETE CASCADE
);

CREATE TABLE NOTE
(
 "ID"            numeric PRIMARY KEY,
 NOTE            text NOT NULL,		-- Text following markdown language. Reference to an entry in a table can be written as `[text](<TABLE_NAME>@<XREF>)`.
 LOCALE          text,					-- Locale as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
 REFERENCE_TABLE text NOT NULL,		-- The table name this record is attached to (ex. "assertion", "citation", "source", "cultural norm", "historic date", "calendar", "event", "repository", "place", "person name", "person", "group", "research status", "media").
 REFERENCE_ID    numeric NOT NULL	-- The ID of the referenced record in the table.
);


-- Media

CREATE TABLE MEDIA
(
 "ID"             numeric PRIMARY KEY,
 IDENTIFIER       text NOT NULL UNIQUE,	-- An identifier for the media (must be unique, ex. a complete local or remote file reference (following RFC 1736 specifications) to the auxiliary data).
 TITLE            text,							-- The name of the media.
 "TYPE"           text,							-- (ex. "photo", "audio", "video", "home movie", "newsreel", "microfilm", "microfiche", "cd-rom")
 PHOTO_PROJECTION text,							-- The projection/mapping/coordinate system of an photo. Known values include "spherical UV", "cylindrical equirectangular horizontal"/"cylindrical equirectangular vertical" (equirectangular photo).
 DATE_ID          numeric,						-- The date this media was first recorded.
 FOREIGN KEY (DATE_ID) REFERENCES HISTORIC_DATE ( "ID" ) ON DELETE SET NULL
);

CREATE TABLE MEDIA_JUNCTION
(
 "ID"            numeric PRIMARY KEY,
 MEDIA_ID        numeric NOT NULL,
 REFERENCE_TABLE text NOT NULL,		-- The table name this record is attached to (ex. "cultural norm", "event", "repository", "source", "citation", "assertion", "place", "note", "person", "person name", "group", "research status").
 REFERENCE_ID    numeric NOT NULL,	-- The ID of the referenced record in the table.
 PHOTO_CROP      text,					-- Top-left coordinate and width-height length of the enclosing box inside an photo.
 FOREIGN KEY (MEDIA_ID) REFERENCES MEDIA ( "ID" ) ON DELETE CASCADE
);


-- Person

-- Name can be attached through a person name.
CREATE TABLE PERSON
(
 "ID"       numeric PRIMARY KEY,
 PHOTO_ID   numeric,	-- The primary photo for this person.
 PHOTO_CROP text,		-- Top-left coordinate and width-height length of the enclosing box inside an photo.
 FOREIGN KEY (PHOTO_ID) REFERENCES MEDIA ( "ID" ) ON DELETE SET NULL
);

-- Transcriptions and transliterations of the name can be attached through a localized text (with type "name").
CREATE TABLE PERSON_NAME
(
 "ID"          numeric PRIMARY KEY,
 PERSON_ID     numeric NOT NULL,
 PERSONAL_NAME text,					-- A verbatim copy of the (primary, that is the proper name) name written in the original language.
 FAMILY_NAME   text,					-- A verbatim copy of the (seconday, that is everything that is not a proper name, like a surname) name written in the original language.
 NAME_LOCALE   text,					-- Locale of the name as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
 "TYPE"        text,					-- (ex. "birth name" (name given on birth certificate), "also known as" (an unofficial pseudonym, also known as, alias, etc), "nickname" (a familiar name), "family nickname", "pseudonym", "legal" (legally changed name), "adoptive name" (name assumed upon adoption), "stage name", "marriage name" (name assumed at marriage), "call name", "official name", "anglicized name", "religious order name", "pen name", "name at work", "immigrant" (name assumed at the time of immigration) -- see https://github.com/FamilySearch/gedcomx/blob/master/specifications/name-part-qualifiers-specification.md)
 FOREIGN KEY (PERSON_ID) REFERENCES PERSON ( "ID" ) ON DELETE CASCADE
);


-- Group

-- A group can be of genealogical, historical, or general interest. Examples of groups that might be useful in genealogy research are households and neighborhoods as found in a census.
CREATE TABLE "GROUP"
(
 "ID"       numeric PRIMARY KEY,
 "TYPE"     text,		-- The type of the group (ex. "family", "neighborhood", "fraternity", "ladies club", "literary society").
 PHOTO_ID   numeric,	-- The primary photo for this group.
 PHOTO_CROP text,		-- Top-left coordinate and width-height length of the enclosing box inside an photo.
 FOREIGN KEY (PHOTO_ID) REFERENCES MEDIA ( "ID" ) ON DELETE SET NULL
);

CREATE TABLE GROUP_JUNCTION
(
 "ID"            numeric PRIMARY KEY,
 GROUP_ID        numeric NOT NULL,
 REFERENCE_TABLE text NOT NULL,		-- The table name this record is attached to (ex. "person", "group", "place").
 REFERENCE_ID    numeric NOT NULL,	-- The ID of the referenced record in the table.
 ROLE            text,					-- What role the referenced entity played in the group that is being cited in this context (ex. "partner", "child", "president", "member", "resident" (in a neighborhood), "head of household", "tribal leader").
 CERTAINTY       text,					-- A status code that allows passing on the users opinion of whether the group exists (ex. "impossible", "unlikely", "possible", "almost certain", "certain").
 CREDIBILITY     text,					-- A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence ("unreliable/estimated data", "questionable reliability of evidence", "secondary evidence, data officially recorded sometime after assertion", "direct and primary evidence used, or by dominance of the evidence").
 FOREIGN KEY (GROUP_ID) REFERENCES "GROUP" ( "ID" ) ON DELETE CASCADE
);


-- Event

/*
1. Assertion can be made about "place", "cultural norm", "historic date", "calendar", "person", "group", "media", "person name".
2. A conclusion is an assertion that is substantiated, different from a bare assertion that is not substantiated.
3. An event is a collection of conclusions/bare assertions about something (a description) happened somewhere ("place") at a certain time ("historic date") to someone ("person", "group") or something ("place", "cultural norm", "calendar", "media", "person name").
*/
CREATE TABLE EVENT
(
 "ID"            numeric PRIMARY KEY,
 "TYPE"          text NOT NULL,		-- (ex. "historic fact", "birth", "sex", "gender", "marriage", "death", "coroner report", "cremation", "burial", "occupation", "imprisonment", "deportation", "invention", "religious conversion", "wedding", "ran away from home", "residence", "autopsy", "divorce", "engagement", "annulment", "separation", "eye color", "hair color", "height", "weight", "build", "complexion", "gender", "race", "ethnic origin", "anecdote", "marks/scars", "disability", "condition", "religion", "education", "able to read", "able to write", "career", "number of children (total)", "number of children (living)", "marital status", "political affiliation", "special talent", "hobby", "nationality", "draft registration", "legal problem", "tobacco use", "alcohol use", "drug problem", "guardianship", "inquest", "relationship", "bar mitzvah", "bas mitzvah", "jury duty", "baptism", "excommunication", "betrothal", "resignation", "naturalization", "marriage license", "christening", "confirmation", "will", "deed", "escrow", "probate", "retirement", "ordination", "graduation", "emigration", "enrollment", "execution", "employment", "land grant", "name change", "land purchase", "land sale", "military induction", "military enlistment", "military rank", "military award", "military promotion", "military service", "military release", "military discharge", "military resignation", "military retirement", "prison", "pardon", "membership", "hospitalization", "illness", "honor", "marriage bann", "missing in action", "adoption", "reburial", "filing for divorce", "exhumation", "funeral", "celebration of life", "partnership", "natural disaster", "blessing", "anniversary celebration", "first communion", "fosterage", "posthumous offspring", "immigration", "marriage contract", "reunion", "scattering of ashes", "inurnment", "cohabitation", "living together", "wedding anniversary", "patent filing", "patent granted", "internment", "learning", "conversion", "travel", "caste", "description", "number of marriages", "property", "imaginary", "marriage settlement", "specialty", "award")
 DESCRIPTION     text,				   -- The description of the event.
 PLACE_ID        numeric,				-- The place this event happened.
 DATE_ID         numeric,				-- The date this event has happened.
 REFERENCE_TABLE text NOT NULL,		-- The table name this record is attached to (ex. "person", "group", "place", "cultural norm", "calendar", "media", "person name").
 REFERENCE_ID    numeric NOT NULL,	-- The ID of the referenced record in the table.
 FOREIGN KEY (PLACE_ID) REFERENCES PLACE ( "ID" ) ON DELETE SET NULL,
 FOREIGN KEY (DATE_ID) REFERENCES HISTORIC_DATE ( "ID" ) ON DELETE SET NULL
);


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
 "ID"          numeric PRIMARY KEY,
 IDENTIFIER    text NOT NULL,	-- An identifier of the rule.
 DESCRIPTION   text,				-- The description of the rule.
 PLACE_ID      numeric,			-- The place this rule applies.
 DATE_START_ID numeric,			-- The date this cultural norm went into effect.
 DATE_END_ID   numeric,			-- The date this cultural norm stopped being in effect.
 CERTAINTY     text,				-- A status code that allows passing on the users opinion of whether the rule is true (ex. "impossible", "unlikely", "possible", "almost certain", "certain").
 CREDIBILITY   text,				-- A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence ("unreliable/estimated data", "questionable reliability of evidence", "secondary evidence, data officially recorded sometime after assertion", "direct and primary evidence used, or by dominance of the evidence").
 FOREIGN KEY (PLACE_ID) REFERENCES PLACE ( "ID" ) ON DELETE SET NULL,
 FOREIGN KEY (DATE_START_ID) REFERENCES HISTORIC_DATE ( "ID" ) ON DELETE SET NULL,
 FOREIGN KEY (DATE_END_ID) REFERENCES HISTORIC_DATE ( "ID" ) ON DELETE SET NULL
);

CREATE TABLE CULTURAL_NORM_JUNCTION
(
 "ID"             numeric PRIMARY KEY,
 CULTURAL_NORM_ID numeric NOT NULL,
 REFERENCE_TABLE  text NOT NULL,		-- The table name this record is attached to (ex. "assertion", "note", "person name", "group").
 REFERENCE_ID     numeric NOT NULL,	-- The ID of the referenced record in the table.
 CERTAINTY        text,					-- A status code that allows passing on the users opinion of whether the connection to the rule is true (ex. "impossible", "unlikely", "possible", "almost certain", "certain").
 CREDIBILITY      text,					-- A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence ("unreliable/estimated data", "questionable reliability of evidence", "secondary evidence, data officially recorded sometime after assertion", "direct and primary evidence used, or by dominance of the evidence").
 FOREIGN KEY (CULTURAL_NORM_ID) REFERENCES CULTURAL_NORM ( "ID" ) ON DELETE CASCADE
);


-- Other application-related things

CREATE TABLE RESTRICTION
(
 "ID"            numeric PRIMARY KEY,
 RESTRICTION     text NOT NULL,		-- Specifies how the record should be treated. Known values and their meaning are: "confidential" (should not be distributed or exported), "public" (can be freely distributed or exported).
 REFERENCE_TABLE text NOT NULL,		-- The table name this record is attached to (ex. "assertion", "citation", "source", "repository", "cultural norm", "historic date", "event", "place", "note", "person name", "person", "group", "media").
 REFERENCE_ID    numeric NOT NULL	-- The ID of the referenced record in the table.
);

CREATE TABLE MODIFICATION
(
 "ID"            numeric PRIMARY KEY,
 REFERENCE_TABLE text NOT NULL,			-- The table name this record is attached to.
 REFERENCE_ID    numeric NOT NULL,		-- The ID of the referenced record in the table.
 CREATION_DATE   timestamp NOT NULL,	-- The creation date of a record.
 UPDATE_DATE     timestamp					-- The changing date of a record.
);

CREATE TABLE RESEARCH_STATUS
(
 "ID"            numeric PRIMARY KEY,
 REFERENCE_TABLE text NOT NULL,		-- The table name this record is attached to.
 REFERENCE_ID    numeric NOT NULL,	-- The ID of the referenced record in the table.
 IDENTIFIER      text NOT NULL,		-- An identifier.
 DESCRIPTION     text,					-- The description of the research status. Text following markdown language. Reference to an entry in a table can be written as `[text](<TABLE_NAME>@<XREF>)`.
 STATUS          text,					-- Research status (ex. "open": recorded but not started yet, "active": currently being searched, "ended": all the information has been found).
 PRIORITY        numeric
);

CREATE TABLE CONTACT
(
 "ID"      numeric PRIMARY KEY,
 CALLER_ID text NOT NULL,	-- Indicates the name of the person associated with this contact.
 NOTE      text				-- Text following markdown language. Reference to an entry in a table can be written as `[text](<TABLE_NAME>@<XREF>)`. Usually it contains a phone number, languages spoken, an electronic address that can be used for contact such as an email address following RFC 5322 specifications, or a World Wide Web page address following RFC 1736 specifications, or any other type of contact address, or other things.
);

CREATE TABLE CONTACT_JUNCTION
(
 "ID"            numeric PRIMARY KEY,
 CONTACT_ID      numeric NOT NULL,
 REFERENCE_TABLE text NOT NULL,		-- The table name this record is attached to.
 REFERENCE_ID    numeric NOT NULL,	-- The ID of the referenced record in the table.
 FOREIGN KEY (CONTACT_ID) REFERENCES CONTACT ( "ID" )
);

CREATE TABLE PROJECT
(
 "ID"             numeric PRIMARY KEY,
 PROTOCOL_NAME    text NOT NULL,			-- "Family LEgacy Format"
 PROTOCOL_VERSION text NOT NULL,			-- "0.0.10"
 COPYRIGHT        text,						-- A copyright statement.
 NOTE             text,						-- Text following markdown language.
 LOCALE           text,						-- Locale as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
 CREATION_DATE    timestamp NOT NULL,	-- The creation date of the project.
 UPDATE_DATE      timestamp				-- The changing date of the project.
);



-- https://wiki.phpgedview.net/en/index.php/Facts_and_Events
-- http://www.gencom.org.nz/GEDCOM_tags.html
-- Life related types are: BIRTH (the exiting of the womb), MIDWIFE, ADOPTION (the creation of a parent-child relationship not associated with birth), CHARACTERISTIC (physical characteristics of a person, in the format `key1: value1, key2: value2`), ANECDOTE, DEATH (the end of life), CHILDREN_COUNT (the reported number of children known to belong to this family, regardless of whether the associated children are represented in the corresponding structure; this is not necessarily the count of children listed in a family structure), MARRIAGES_COUNT, CORONER_REPORT (the act of including an individual in a report by a coroner, a public official, on the investigation into the causes and circumstances of any death which occurred through violence or suddenly with marks of suspicion), BURIAL (the depositing of the body (in whole or in part) of the deceased), CREMATION (the burning of the body (in whole or in part) of the deceased).
-- Family related types are: ENGAGEMENT (the agreement of a couple to enter into a marriage in the future), MARRIAGE_BANN (a public notice of an intent to marry), MARRIAGE_CONTRACT (a formal contractual agreement to marry), MARRIAGE_LICENCE (obtaining a legal license to marry), MARRIAGE_SETTLEMENT (a legal arrangement to modify property rights upon marriage), MARRIAGE (the creation of a family unit (via a legal, religious, customary, common-law, or other form of union)), DIVORCE_FILED (the legal action expressing intent to divorce), DIVORCE_DECREE (an individual's participation in a decree of a court marking the legal separation of a man and woman, totally dissolving the marriage relation), DIVORCE (the ending of a marriage between still-living individuals), ANNULMENT (declaring a marriage to be invalid, as though it had never occurred).
-- Achievements related types are: RESIDENCE, EDUCATION (an educational degree or attainment), GRADUATION (the conclusion of formal education), OCCUPATION (what this person does as a livelihood), RETIREMENT (the cessation of gainful employment, typically because sufficient wealth has been accumulated to no longer necessitate such), MILITARY_AWARD (the act of receiving a medal or honor for military service in a particular campaign or for a particular service), MILITARY_DISCHARGE (a release from serving in the armed forces), MILITARY_INDUCTION (to take into the armed forces), MILITARY_MUSTER_ROLL (the act of including an individual in a list or account of the enlisted persons in a military or naval unit), MILITARY_SERVICE (the act of serving in the armed forces), MILITARY_RANK (the military rank acquired by an individual), MILITARY_RELEASE (the act of releasing from active to inactive military duty), MILITARY_RESIGNATION (the act of resigning from serving in the military; resigning a commission), MILITARY_RETIREMENT (to retire or withdraw from active duty in the armed forces), PRISON (the act of punishment imposed by law or otherwise in the course of administration of justice), PARDON (the act of exempting an individual from the punishment the law inflicts for a crime that person has committed), MEMBERSHIP (the act of becoming a member; joining a group), JURY_DUTY (the act of serving on a jury), MEDICAL, HOSPITALIZATION, ILLNESS (the act of losing good health; sickness; disease), HONOR (to be recognized for an achievement), HOLOCAUST_LIBERATION (the liberation of the Holocaust survivor from place of internment), HOLOCAUST_DEPORTATION (deportation of Holocaust victim/survivor from place of residence), HOLOCAUST_DEPARTURE (the departure of the Holocaust survivor from the place of internment), HOLOCAUST_ARRIVAL (the arrival of a Holocaust victim/survivor to the place of internment) EMANCIPATION (the emancipation of a minor child by its parents, which involves an entire surrender of the right to the care, custody, and earnings of such child as well as the renunciation of parental duties), BANKRUPTCY (the act of taking possession by the trustee of property of the bankrupt).
-- National/government related types are: CASTE (the social, religious, or racial caste tow which an individual belongs), NATIONALITY (a group to which a person is associated, typically by birth, like nation or tribe), EMIGRATION (the departure from the nation or land in which one has nativity or citizenship), IMMIGRATION (the entering of a nation or land in which one does not have nativity or citizenship), NATURALIZATION (the gaining of citizenship in a new nation or land), CENSUS (an inventory of persons or households in a population), SSN (Social Security Number).
-- Possessions and titles related types are: POSSESSION (a list of objects or land owned by the person), TITLE (a title given a person associated with a local or national notion of nobility or royalty), WILL (the creation of a legal document regarding the disposition of a person’s estate upon death), PROBATE (the judicial actions associated with the disposition of the estate of the deceased), DEED (the judicial actions associated with the transferring (conveyancing) the title to a property), GUARDIANSHIP (the act of legally appointing an individual to take care of the affairs of someone who is young or cannot take care of her/himself), ESCROW (an individual's participation in a deed or bond held by a third party until certain conditions are met by other parties), CHANCERY (the resolution through impartial justice between two parties whose claims conflict).
-- Religious and social related types are: RELIGION (the name of a religion with which the event was affiliated).
-- Custom type: <EVENT_TYPE> (a descriptive word or phrase used to further classify the parent event or the attribute tag).
