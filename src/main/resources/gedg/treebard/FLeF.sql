-- Assertion - Citation - Source - Repository

CREATE TABLE ASSERTION
(
 "ID"            numeric PRIMARY KEY,
 CITATION_ID     numeric NOT NULL,
 REFERENCE_TABLE text NOT NULL,
 REFERENCE_ID    numeric NOT NULL,
 ROLE            text NULL,
 CERTAINTY       text NULL,
 CREDIBILITY     numeric NULL,
 FOREIGN KEY (CITATION_ID) REFERENCES CITATION ( "ID" )
);
-- REFERENCE: "place", "cultural norm", "date", "repository", "person", "group", "media", "person name"

CREATE TABLE CITATION
(
 "ID"         numeric PRIMARY KEY,
 SOURCE_ID    numeric NOT NULL,
 LOCATION     text NULL,
 EXTRACT_ID   numeric NULL,
 EXTRACT_TYPE text NULL,
 FOREIGN KEY (SOURCE_ID) REFERENCES SOURCE ( "ID" ),
 FOREIGN KEY (EXTRACT_ID) REFERENCES LOCALIZED_TEXT ( "ID" )
);

CREATE TABLE "SOURCE"
(
 "ID"          numeric PRIMARY KEY,
 IDENTIFIER    text NOT NULL UNIQUE,
 SOURCE_TYPE   text NULL,
 AUTHOR        text NULL,
 PLACE_ID      numeric NULL,
 DATE_ID       numeric NULL,
 REPOSITORY_ID numeric NULL,
 LOCATION      text NULL,
 FOREIGN KEY (PLACE_ID) REFERENCES HISTORIC_PLACE ( "ID" ),
 FOREIGN KEY (DATE_ID) REFERENCES HISTORIC_DATE ( "ID" ),
 FOREIGN KEY (REPOSITORY_ID) REFERENCES REPOSITORY ( "ID" )
);

CREATE TABLE REPOSITORY
(
 "ID"       numeric PRIMARY KEY,
 IDENTIFIER text NOT NULL UNIQUE,
 TYPE       text NULL,
 PERSON_ID  numeric NULL,
 FOREIGN KEY (PERSON_ID) REFERENCES PERSON ( "ID" )
);


-- Date

CREATE TABLE HISTORIC_DATE
(
 "ID"                 numeric PRIMARY KEY,
 "DATE"               text NOT NULL,
 CALENDAR_ID          numeric NULL,
 DATE_ORIGINAL        text NULL,
 CALENDAR_ORIGINAL_ID numeric NULL,
 CERTAINTY            text NULL,
 CREDIBILITY          numeric NULL,
 FOREIGN KEY (CALENDAR_ID) REFERENCES CALENDAR ( "ID" ),
 FOREIGN KEY (CALENDAR_ORIGINAL_ID) REFERENCES CALENDAR ( "ID" )
);

CREATE TABLE CALENDAR
(
 "ID" numeric PRIMARY KEY,
 TYPE text NOT NULL
);


-- Place

CREATE TABLE PLACE
(
 "ID"                   numeric PRIMARY KEY,
 IDENTIFIER             text NOT NULL UNIQUE,
 NAME_ID                numeric NOT NULL,
 TYPE                   text NULL,
 COORDINATE             text NULL,
 COORDINATE_TYPE        text NULL,
 COORDINATE_CREDIBILITY numeric NULL,
 PRIMARY_PLACE_ID       numeric NULL,
 IMAGE_ID               numeric NULL,
 IMAGE_CROP             text NULL,
 FOREIGN KEY (NAME_ID) REFERENCES LOCALIZED_TEXT ( "ID" ),
 FOREIGN KEY (PRIMARY_PLACE_ID) REFERENCES PLACE ( "ID" ),
 FOREIGN KEY (IMAGE_ID) REFERENCES MEDIA ( "ID" )
);

CREATE TABLE HISTORIC_PLACE
(
 "ID"        numeric PRIMARY KEY,
 PLACE_ID    numeric NOT NULL,
 CERTAINTY   text NULL,
 CREDIBILITY numeric NULL,
 FOREIGN KEY (PLACE_ID) REFERENCES PLACE ( "ID" )
);


--- Localized text - Note

CREATE TABLE LOCALIZED_TEXT
(
 "ID"               numeric PRIMARY KEY,
 TEXT               text NOT NULL,
 LOCALE             text NULL,
 TYPE               text NULL,
 TRANSCRIPTION      text NULL,
 TRANSCRIPTION_TYPE text NULL,
 REFERENCE_TABLE    text NULL,
 REFERENCE_ID       numeric NULL
);
-- REFERENCE: "citation", "person name", "place"

CREATE TABLE NOTE
(
 "ID"            numeric PRIMARY KEY,
 NOTE            text NOT NULL,
 REFERENCE_TABLE text NULL,
 REFERENCE_ID    numeric NULL
);
-- REFERENCE: "project", "assertion", "citation", "source", "cultural norm", "historic date", "calendar", "event", "repository", "historic place", "place", "person name", "person", "group", "research status", "media"


-- Media

CREATE TABLE MEDIA
(
 "ID"             numeric PRIMARY KEY,
 IDENTIFIER       text NOT NULL UNIQUE,
 TITLE            text NULL,
 TYPE             text NULL,
 IMAGE_PROJECTION text NULL
);

CREATE TABLE MEDIA_JUNCTION
(
 "ID"            numeric PRIMARY KEY,
 MEDIA_ID        numeric NOT NULL,
 REFERENCE_TABLE text NOT NULL,
 REFERENCE_ID    numeric NOT NULL,
 FOREIGN KEY (MEDIA_ID) REFERENCES MEDIA ( "ID" )
);
-- REFERENCE: "cultural norm", "event", "repository", "source", "citation", "assertion", "place", "note", "person name", "person", "group", "media", "research status"


-- Person

CREATE TABLE PERSON
(
 "ID"       numeric PRIMARY KEY,
 IMAGE_ID   numeric NULL,
 IMAGE_CROP text NULL,
 FOREIGN KEY (IMAGE_ID) REFERENCES MEDIA ( "ID" )
);

CREATE TABLE PERSON_NAME
(
 "ID"                   numeric PRIMARY KEY,
 PERSON_ID              numeric NOT NULL,
 NAME_ID                numeric NULL,
 TYPE                   text NULL,
 ALTERNATIVE_SORT_ORDER text NULL,
 CONSTRAINT PRIMARY KEYPK_1 PRIMARY KEY ( "ID" ),
 FOREIGN KEY (PERSON_ID) REFERENCES PERSON ( "ID" ),
 FOREIGN KEY (NAME_ID) REFERENCES PERSON_NAME ( "ID" )
);


-- Group

CREATE TABLE "GROUP"
(
 "ID"       numeric PRIMARY KEY,
 TYPE       text NULL,
 IMAGE_ID   numeric NULL,
 IMAGE_CROP text NULL,
 FOREIGN KEY (IMAGE_ID) REFERENCES MEDIA ( "ID" )
);

CREATE TABLE GROUP_JUNCTION
(
 "ID"            numeric PRIMARY KEY,
 GROUP_ID        numeric NOT NULL,
 REFERENCE_TABLE text NOT NULL,
 REFERENCE_ID    numeric NOT NULL,
 ROLE            text NULL,
 CERTAINTY       text NULL,
 CREDIBILITY     numeric NULL,
 FOREIGN KEY (GROUP_ID) REFERENCES "GROUP" ( "ID" )
);
-- REFERENCE: "person", "group"


-- Event

CREATE TABLE EVENT
(
 "ID"            numeric PRIMARY KEY,
 EVENT_TYPE      text NOT NULL,
 DESCRIPTION     text NULL,
 PLACE_ID        numeric NULL,
 DATE_ID         numeric NULL,
 REFERENCE_TABLE text NOT NULL,
 REFERENCE_ID    numeric NOT NULL,
 FOREIGN KEY (PLACE_ID) REFERENCES HISTORIC_PLACE ( "ID" ),
 FOREIGN KEY (DATE_ID) REFERENCES HISTORIC_DATE ( "ID" )
);
-- REFERENCE: "person", "group", "place", "repository", "cultural norm", "calendar", "media", "person name"


-- Cultural norm

CREATE TABLE CULTURAL_NORM
(
 "ID"        numeric PRIMARY KEY,
 DESCRIPTION text NOT NULL,
 PLACE_ID    numeric NULL,
 DATE_ID     numeric NULL,
 CERTAINTY   text NULL,
 CREDIBILITY numeric NULL,
 FOREIGN KEY (PLACE_ID) REFERENCES HISTORIC_PLACE ( "ID" ),
 FOREIGN KEY (DATE_ID) REFERENCES HISTORIC_DATE ( "ID" )
);

CREATE TABLE CULTURAL_NORM_JUNCTION
(
 "ID"             numeric PRIMARY KEY,
 CULTURAL_NORM_ID numeric NOT NULL,
 REFERENCE_TABLE  text NOT NULL,
 REFERENCE_ID     numeric NOT NULL,
 CERTAINTY        text NULL,
 CREDIBILITY      numeric NULL,
 FOREIGN KEY (CULTURAL_NORM_ID) REFERENCES CULTURAL_NORM ( "ID" )
);
-- REFERENCE: "assertion", "event", "note", "person", "person name", "group"


-- Other application-related things

CREATE TABLE PROJECT
(
 "ID"             numeric PRIMARY KEY,
 PROTOCOL_NAME    text NOT NULL,
 PROTOCOL_VERSION text NOT NULL,
 COPYRIGHT        text NULL,
 NOTE             text NULL
);

CREATE TABLE RESTRICTION
(
 "ID"            numeric PRIMARY KEY,
 REFERENCE_TABLE text NOT NULL,
 REFERENCE_ID    numeric NOT NULL,
 RESTRICTION     text NOT NULL
);

CREATE TABLE MODIFICATION
(
 "ID"            numeric PRIMARY KEY,
 REFERENCE_TABLE text NOT NULL,
 REFERENCE_ID    numeric NOT NULL,
 CREATION_DATE   date NOT NULL,
 UPDATE_DATE     date NULL
);

CREATE TABLE RESEARCH_STATUS
(
 "ID"            numeric PRIMARY KEY,
 REFERENCE_TABLE text NOT NULL,
 REFERENCE_ID    numeric NOT NULL,
 DESCRIPTION     text NOT NULL,
 STATUS          text NULL,
 PRIORITY        numeric NULL
);

CREATE TABLE CONTACT
(
 "ID"      numeric PRIMARY KEY,
 CALLER_ID text NOT NULL,
 NOTE      text NULL
);

CREATE TABLE CONTACT_JUNCTION
(
 "ID"            numeric PRIMARY KEY,
 CONTACT_ID      numeric NOT NULL,
 REFERENCE_TABLE text NOT NULL,
 REFERENCE_ID    numeric NOT NULL,
 FOREIGN KEY (CONTACT_ID) REFERENCES CONTACT ( "ID" )
);
