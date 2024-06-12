CREATE TABLE "assertion"
  (
     assertion_id INTEGER PRIMARY KEY,
     citation_id  INTEGER NOT NULL,
     event_id     INTEGER,
     name_id      INTEGER,
     dates        TEXT NOT NULL DEFAULT '',
     places       TEXT NOT NULL DEFAULT '',
     particulars  TEXT NOT NULL DEFAULT '',
     ages         TEXT NOT NULL DEFAULT '',
     names        TEXT NOT NULL DEFAULT '',
     roles        TEXT NOT NULL DEFAULT '',
     surety       FLOAT DEFAULT NULL,
     FOREIGN KEY (citation_id) REFERENCES citation (citation_id),
     FOREIGN KEY (event_id) REFERENCES event (event_id),
     FOREIGN KEY (name_id) REFERENCES NAME (name_id)
  );

CREATE TABLE "citation"
  (
     citation_id INTEGER PRIMARY KEY,
     source_id   INTEGER NOT NULL,
     citations   TEXT,
     FOREIGN KEY (source_id) REFERENCES source (source_id)
  );

CREATE TABLE "source"
  (
     source_id      INTEGER PRIMARY KEY,
     sources        TEXT UNIQUE,
     source_type_id INTEGER REFERENCES source_type (source_type_id),
     author         TEXT NOT NULL DEFAULT '',
     description    TEXT NOT NULL DEFAULT ''
  );


CREATE TABLE "change_date"
  (
     change_date_id  INTEGER PRIMARY KEY,
     change_dates    TEXT NOT NULL DEFAULT '00 month, 0000',
     change_times    TEXT DEFAULT '',
     person_id       INTEGER UNIQUE DEFAULT NULL,
     source_id       INTEGER UNIQUE DEFAULT NULL,
     note_id         INTEGER UNIQUE DEFAULT NULL,
     media_id        INTEGER UNIQUE DEFAULT NULL,
     repository_id   INTEGER UNIQUE DEFAULT NULL,
     couple_id       INTEGER UNIQUE DEFAULT NULL,
     contact_id      INTEGER UNIQUE DEFAULT NULL,
     place_id        INTEGER UNIQUE DEFAULT NULL,
     place_name_id   INTEGER UNIQUE DEFAULT NULL,
     nested_place_id INTEGER UNIQUE DEFAULT NULL,
     name_id         INTEGER UNIQUE DEFAULT NULL,
     citation_id     INTEGER UNIQUE DEFAULT NULL,
     event_id        INTEGER UNIQUE DEFAULT NULL,
     assertion_id    INTEGER UNIQUE DEFAULT NULL,
     FOREIGN KEY (person_id) REFERENCES person (person_id),
     FOREIGN KEY (source_id) REFERENCES source (source_id),
     FOREIGN KEY (note_id) REFERENCES note (note_id),
     FOREIGN KEY (media_id) REFERENCES media (media_id),
     FOREIGN KEY (repository_id) REFERENCES repository (repository_id),
     FOREIGN KEY (couple_id) REFERENCES couple (couple_id),
     FOREIGN KEY (contact_id) REFERENCES contact (contact_id),
     FOREIGN KEY (place_id) REFERENCES place (place_id),
     FOREIGN KEY (place_name_id) REFERENCES place_name (place_name_id),
     FOREIGN KEY (nested_place_id) REFERENCES nested_place (nested_place_id),
     FOREIGN KEY (name_id) REFERENCES NAME (name_id),
     FOREIGN KEY (citation_id) REFERENCES citation (citation_id),
     FOREIGN KEY (event_id) REFERENCES event (event_id),
     FOREIGN KEY (assertion_id) REFERENCES assertion (assertion_id)
  );

CREATE TABLE "chart"
  (
     chart_id      INTEGER PRIMARY KEY,
     chart_name    TEXT NOT NULL DEFAULT '',
     chart_type_id INTEGER REFERENCES chart_type (chart_type_id)
  );

CREATE TABLE "chart_type"
  (
     chart_type_id INTEGER PRIMARY KEY,
     chart_types   TEXT UNIQUE NOT NULL,
     built_in      BOOLEAN DEFAULT 1,
     hidden        BOOLEAN DEFAULT 0
  );

CREATE TABLE "colors_type"
  (
     colors_type_id INTEGER PRIMARY KEY,
     color1         TEXT NOT NULL,
     color2         TEXT NOT NULL,
     color3         TEXT NOT NULL,
     color4         TEXT NOT NULL,
     built_in       BOOLEAN NOT NULL DEFAULT 1,
     hidden         BOOLEAN NOT NULL DEFAULT 0
  );

CREATE TABLE "contact"
  (
     contact_id INTEGER PRIMARY KEY,
     contacts   TEXT UNIQUE,
     position   TEXT NOT NULL DEFAULT '',
     company    TEXT NOT NULL DEFAULT '',
     email      TEXT NOT NULL DEFAULT '',
     address    TEXT NOT NULL DEFAULT '',
     phone      TEXT NOT NULL DEFAULT '',
     cell       TEXT NOT NULL DEFAULT '',
     website    TEXT NOT NULL DEFAULT '',
     blog       TEXT NOT NULL DEFAULT '',
     forum      TEXT NOT NULL DEFAULT '',
     private    BOOLEAN NOT NULL DEFAULT 1,
     language   TEXT NOT NULL DEFAULT '',
     submitted  TEXT NOT NULL DEFAULT '',
     detail     TEXT NOT NULL DEFAULT ''
  );

CREATE TABLE "couple"
  (
     couple_id          INTEGER PRIMARY KEY,
     person_id1         INTEGER DEFAULT NULL,
     person_id2         INTEGER DEFAULT NULL,
     family_description TEXT NOT NULL DEFAULT '',
     FOREIGN KEY (person_id1) REFERENCES person (person_id),
     FOREIGN KEY (person_id2) REFERENCES person (person_id)
  );

CREATE TABLE "current"
  (
     current_id      INTEGER PRIMARY KEY,
     nested_place_id INTEGER,
     person_id       INTEGER,
     citation_id     INTEGER REFERENCES citation (citation_id),
     source_id       INTEGER REFERENCES source (source_id),
     image_directory TEXT DEFAULT '',
     FOREIGN KEY (person_id) REFERENCES person (person_id),
     FOREIGN KEY (nested_place_id) REFERENCES nested_place (nested_place_id)
  );

CREATE TABLE "date_format"
  (
     date_format_id INTEGER PRIMARY KEY,
     date_formats   TEXT NOT NULL DEFAULT 'alpha_dmy',
     abt            TEXT NOT NULL DEFAULT 'abt',
     est            TEXT NOT NULL DEFAULT 'est',
     cal            TEXT NOT NULL DEFAULT 'calc',
     bef_aft        TEXT NOT NULL DEFAULT 'bef/aft',
     bc_ad          TEXT NOT NULL DEFAULT 'BCE/CE',
     os_ns          TEXT NOT NULL DEFAULT 'OS/NS',
     span           TEXT NOT NULL DEFAULT 'from_to',
     range          TEXT NOT NULL DEFAULT 'btwn_&'
  );

CREATE TABLE demo
  (
     id   INTEGER PRIMARY KEY,
     NAME VARCHAR(20),
     hint TEXT
  );

CREATE TABLE "event"
  (
     event_id        INTEGER PRIMARY KEY,
     date            TEXT NOT NULL DEFAULT '-0000-00-00-------',
     particulars     TEXT NOT NULL DEFAULT '',
     age             TEXT NOT NULL DEFAULT '',
     person_id       INTEGER DEFAULT NULL,
     event_type_id   INTEGER NOT NULL,
     date_sorter     TEXT NOT NULL DEFAULT '0,0,0',
     nested_place_id INTEGER NOT NULL DEFAULT 1,
     couple_id       INTEGER DEFAULT NULL REFERENCES couple (couple_id),
     age1            TEXT NOT NULL DEFAULT '',
     age2            TEXT NOT NULL DEFAULT '',
     FOREIGN KEY (person_id) REFERENCES person (person_id),
     FOREIGN KEY (nested_place_id) REFERENCES nested_place (nested_place_id),
     FOREIGN KEY (event_type_id) REFERENCES event_type (event_type_id)
  );

CREATE TABLE "event_type"
  (
     event_type_id INTEGER PRIMARY KEY,
     event_types   TEXT UNIQUE NOT NULL,
     built_in      BOOLEAN NOT NULL DEFAULT 0,
     hidden        BOOLEAN NOT NULL DEFAULT 0,
     couple        BOOLEAN NOT NULL DEFAULT 0,
     after_death   BOOLEAN NOT NULL DEFAULT 0,
     marital       BOOLEAN NOT NULL DEFAULT 0
  );

CREATE TABLE "font_preference"
  (
     format_id           INTEGER PRIMARY KEY,
     output_font         TEXT,
     input_font          TEXT,
     font_size           INTEGER,
     default_output_font TEXT NOT NULL DEFAULT 'courier',
     default_input_font  TEXT NOT NULL DEFAULT 'dejavu sans mono',
     default_font_size   INTEGER NOT NULL DEFAULT 12
  );

CREATE TABLE handle
  (
     handle_id     INTEGER PRIMARY KEY,
     contact_id    INTEGER NOT NULL,
     repository_id INTEGER,
     handles       TEXT NOT NULL,
     FOREIGN KEY (contact_id) REFERENCES contact (contact_id),
     FOREIGN KEY (repository_id) REFERENCES repository (repository_id)
  );

CREATE TABLE "kin_type"
  (
     kin_type_id      INTEGER PRIMARY KEY,
     kin_types        TEXT UNIQUE NOT NULL,
     abbrev_kin_types TEXT,
     built_in         BOOLEAN NOT NULL DEFAULT 1,
     hidden           BOOLEAN NOT NULL DEFAULT 0
  );

CREATE TABLE "locator"
  (
     locator_id      INTEGER PRIMARY KEY,
     locators        TEXT,
     locator_type_id INTEGER,
     person_id       INTEGER REFERENCES person (person_id),
     couple_id       INTEGER REFERENCES couple (couple_id),
     media_id        INTEGER REFERENCES media (media_id),
     note_id         INTEGER REFERENCES note (note_id),
     repository_id   INTEGER REFERENCES repository (repository_id),
     source_id       INTEGER REFERENCES source (source_id),
     contact_id      INTEGER REFERENCES contact (contact_id),
     citation_id     INTEGER REFERENCES citation (citation_id),
     assertion_id    INTEGER REFERENCES assertion (assertion_id),
     FOREIGN KEY (locator_type_id) REFERENCES locator_type (locator_type_id)
  );

CREATE TABLE "locator_type"
  (
     locator_type_id INTEGER PRIMARY KEY,
     locator_types   TEXT NOT NULL,
     built_in        BOOLEAN DEFAULT 1,
     hidden          BOOLEAN DEFAULT 0,
     abbreviation    TEXT DEFAULT NULL
  );

CREATE TABLE "media"
  (
     media_id      INTEGER PRIMARY KEY,
     file_names    TEXT UNIQUE NOT NULL,
     captions      TEXT,
     titles        TEXT DEFAULT '',
     media_type_id INTEGER DEFAULT 1 REFERENCES media_type (media_type_id)
  );

CREATE TABLE "media_links"
  (
     media_links_id  INTEGER PRIMARY KEY,
     main_image      BOOLEAN DEFAULT 0,
     person_id       INTEGER DEFAULT NULL,
     couple_id       INTEGER DEFAULT NULL,
     place_id        INTEGER DEFAULT NULL,
     source_id       INTEGER DEFAULT NULL,
     citation_id     INTEGER DEFAULT NULL,
     event_id        INTEGER DEFAULT NULL,
     assertion_id    INTEGER DEFAULT NULL,
     name_id         INTEGER DEFAULT NULL,
     chart_id        INTEGER DEFAULT NULL,
     contact_id      INTEGER DEFAULT NULL,
     repository_id   INTEGER DEFAULT NULL,
     project_id      INTEGER DEFAULT NULL,
     to_do_id        INTEGER DEFAULT NULL,
     report_id       INTEGER DEFAULT NULL,
     media_id        INTEGER DEFAULT NULL REFERENCES media (media_id),
     nested_place_id INTEGER DEFAULT NULL REFERENCES nested_place (
     nested_place_id),
     FOREIGN KEY (person_id) REFERENCES person (person_id),
     FOREIGN KEY (couple_id) REFERENCES couple (couple_id),
     FOREIGN KEY (place_id) REFERENCES place (place_id),
     FOREIGN KEY (source_id) REFERENCES source (source_id),
     FOREIGN KEY (citation_id) REFERENCES citation (citation_id),
     FOREIGN KEY (event_id) REFERENCES event (event_id),
     FOREIGN KEY (assertion_id) REFERENCES assertion (assertion_id),
     FOREIGN KEY (name_id) REFERENCES NAME (name_id),
     FOREIGN KEY (chart_id) REFERENCES chart (chart_id),
     FOREIGN KEY (contact_id) REFERENCES contact (contact_id),
     FOREIGN KEY (repository_id) REFERENCES repository (repository_id),
     FOREIGN KEY (project_id) REFERENCES project (project_id),
     FOREIGN KEY (to_do_id) REFERENCES to_do (to_do_id),
     FOREIGN KEY (report_id) REFERENCES report (report_id)
  );

CREATE TABLE "media_type"
  (
     media_type_id INTEGER PRIMARY KEY,
     media_types   TEXT UNIQUE NOT NULL,
     built_in      BOOLEAN DEFAULT 1,
     hidden        BOOLEAN DEFAULT 0
  );

CREATE TABLE "name"
  (
     name_id      INTEGER PRIMARY KEY,
     person_id    INTEGER NOT NULL,
     names        TEXT NOT NULL,
     name_type_id INTEGER NOT NULL DEFAULT 18,
     sort_order   TEXT NOT NULL,
     used_by      TEXT NOT NULL DEFAULT '',
     FOREIGN KEY (person_id) REFERENCES person (person_id),
     FOREIGN KEY (name_type_id) REFERENCES name_type (name_type_id)
  );

CREATE TABLE "name_type"
  (
     name_type_id INTEGER PRIMARY KEY,
     name_types   TEXT UNIQUE NOT NULL,
     hierarchy    INTEGER DEFAULT 999,
     built_in     BOOLEAN DEFAULT 1,
     hidden       BOOLEAN DEFAULT 0
  );

CREATE TABLE "nested_place"
  (
     nested_place_id INTEGER PRIMARY KEY,
     nest0           INTEGER NOT NULL DEFAULT 1,
     nest1           INTEGER NOT NULL DEFAULT 1,
     nest2           INTEGER NOT NULL DEFAULT 1,
     nest3           INTEGER NOT NULL DEFAULT 1,
     nest4           INTEGER NOT NULL DEFAULT 1,
     nest5           INTEGER NOT NULL DEFAULT 1,
     nest6           INTEGER NOT NULL DEFAULT 1,
     nest7           INTEGER NOT NULL DEFAULT 1,
     nest8           INTEGER NOT NULL DEFAULT 1,
     FOREIGN KEY (nest0) REFERENCES place (place_id),
     FOREIGN KEY (nest1) REFERENCES place (place_id),
     FOREIGN KEY (nest2) REFERENCES place (place_id),
     FOREIGN KEY (nest3) REFERENCES place (place_id),
     FOREIGN KEY (nest4) REFERENCES place (place_id),
     FOREIGN KEY (nest5) REFERENCES place (place_id),
     FOREIGN KEY (nest6) REFERENCES place (place_id),
     FOREIGN KEY (nest7) REFERENCES place (place_id),
     FOREIGN KEY (nest8) REFERENCES place (place_id),
     UNIQUE (nest0, nest1, nest2, nest3, nest4, nest5, nest6, nest7, nest8)
  );

CREATE TABLE "note"
  (
     note_id INTEGER PRIMARY KEY,
     notes   TEXT NOT NULL DEFAULT '',
     private BOOLEAN NOT NULL DEFAULT 0
  );

CREATE TABLE "notes_links"
  (
     notes_links_id   INTEGER PRIMARY KEY,
     note_id          INTEGER,
     note_topic       TEXT DEFAULT NULL COLLATE nocase,
     note_topic_order INTEGER,
     person_id        INTEGER,
     place_id         INTEGER,
     place_name_id    INTEGER,
     name_id          INTEGER,
     source_id        INTEGER,
     citation_id      INTEGER,
     event_id         INTEGER,
     assertion_id     INTEGER,
     project_id       INTEGER,
     to_do_id         INTEGER,
     contact_id       INTEGER,
     repository_id    INTEGER,
     report_id        INTEGER,
     chart_id         INTEGER,
     media_id         INTEGER,
     couple_id        INTEGER REFERENCES couple (couple_id),
     FOREIGN KEY (person_id) REFERENCES person (person_id),
     FOREIGN KEY (place_id) REFERENCES place (place_id),
     FOREIGN KEY (place_name_id) REFERENCES place_name (place_name_id),
     FOREIGN KEY (name_id) REFERENCES NAME (name_id),
     FOREIGN KEY (source_id) REFERENCES source (source_id),
     FOREIGN KEY (citation_id) REFERENCES citation (citation_id),
     FOREIGN KEY (event_id) REFERENCES event (event_id),
     FOREIGN KEY (assertion_id) REFERENCES assertion (assertion_id),
     FOREIGN KEY (project_id) REFERENCES project (project_id),
     FOREIGN KEY (to_do_id) REFERENCES to_do (to_do_id),
     FOREIGN KEY (contact_id) REFERENCES contact (contact_id),
     FOREIGN KEY (media_id) REFERENCES media (media_id),
     FOREIGN KEY (note_id) REFERENCES note (note_id),
     FOREIGN KEY (repository_id) REFERENCES repository (repository_id),
     FOREIGN KEY (report_id) REFERENCES report (report_id),
     FOREIGN KEY (chart_id) REFERENCES chart (chart_id),
     UNIQUE (note_id, note_topic, person_id),
     UNIQUE (note_id, note_topic, place_id),
     UNIQUE (note_id, note_topic, place_name_id),
     UNIQUE (note_id, note_topic, name_id),
     UNIQUE (note_id, note_topic, source_id),
     UNIQUE (note_id, note_topic, citation_id),
     UNIQUE (note_id, note_topic, event_id),
     UNIQUE (note_id, note_topic, assertion_id),
     UNIQUE (note_id, note_topic, project_id),
     UNIQUE (note_id, note_topic, to_do_id),
     UNIQUE (note_id, note_topic, contact_id),
     UNIQUE (note_id, note_topic, repository_id),
     UNIQUE (note_id, note_topic, report_id),
     UNIQUE (note_id, note_topic, chart_id),
     UNIQUE (note_id, note_topic, media_id),
     UNIQUE (note_id, note_topic, couple_id)
  );

CREATE TABLE person
  (
     person_id INTEGER PRIMARY KEY,
     gender    TEXT NOT NULL DEFAULT 'unknown'
  );

CREATE TABLE "place"
  (
     place_id              INTEGER PRIMARY KEY,
     latitude              TEXT DEFAULT '',
     longitude             TEXT DEFAULT '',
     cartesian_coordinates TEXT DEFAULT '',
     township              TEXT DEFAULT '',
     range                 TEXT DEFAULT '',
     section               TEXT DEFAULT '',
     legal_subdivision     TEXT DEFAULT '',
     hint                  TEXT DEFAULT NULL,
     check_dupes           BOOLEAN DEFAULT 1
  );

CREATE TABLE places_types
  (
     places_types_id     INTEGER PRIMARY KEY,
     place_id            INTEGER,
     place_name_id       INTEGER,
     date_of_creation    TEXT NOT NULL DEFAULT '',
     date_of_dissolution TEXT NOT NULL DEFAULT '',
     place_type_id       INTEGER,
     FOREIGN KEY (place_id) REFERENCES place (place_id),
     FOREIGN KEY (place_name_id) REFERENCES place_name (place_name_id),
     FOREIGN KEY (place_type_id) REFERENCES place_type (place_type_id)
  );

CREATE TABLE place_name
  (
     place_name_id   INTEGER PRIMARY KEY,
     place_names     TEXT NOT NULL,
     place_id        INTEGER REFERENCES place (place_id),
     main_place_name BOOLEAN NOT NULL DEFAULT 0
  );

CREATE TABLE "place_type"
  (
     place_type_id INTEGER PRIMARY KEY,
     place_types   TEXT UNIQUE NOT NULL,
     description   TEXT DEFAULT '',
     built_in      BOOLEAN DEFAULT 1,
     hidden        BOOLEAN DEFAULT 0
  );

CREATE TABLE preferences
  (
     preferences_id     INTEGER PRIMARY KEY,
     use_default_images BOOLEAN
  );

CREATE TABLE project
  (
     project_id      INTEGER PRIMARY KEY,
     projects        TEXT NOT NULL DEFAULT '',
     project_summary TEXT
  );

CREATE TABLE "report"
  (
     report_id      INTEGER PRIMARY KEY,
     report_name    TEXT NOT NULL DEFAULT '',
     report_type_id INTEGER REFERENCES report_type (report_type_id)
  );

CREATE TABLE "report_type"
  (
     report_type_id INTEGER PRIMARY KEY,
     report_types   TEXT UNIQUE NOT NULL,
     built_in       BOOLEAN DEFAULT 1,
     hidden         BOOLEAN DEFAULT 0
  );

CREATE TABLE repositories_links
  (
     repositories_links_id INTEGER PRIMARY KEY,
     repository_type_id    INTEGER DEFAULT NULL,
     source_id             INTEGER DEFAULT NULL,
     citation_id           INTEGER DEFAULT NULL,
     repository_id         INTEGER DEFAULT NULL,
     locator_id            INTEGER DEFAULT NULL,
     media_id              INTEGER DEFAULT NULL,
     contact_id            INTEGER DEFAULT NULL,
     FOREIGN KEY (repository_type_id) REFERENCES repository_type (
     repository_type_id),
     FOREIGN KEY (source_id) REFERENCES source (source_id),
     FOREIGN KEY (citation_id) REFERENCES citation (citation_id),
     FOREIGN KEY (repository_id) REFERENCES repository (repository_id),
     FOREIGN KEY (locator_id) REFERENCES locator (locator_id),
     FOREIGN KEY (media_id) REFERENCES media (media_id),
     FOREIGN KEY (contact_id) REFERENCES contact (contact_id)
  );

CREATE TABLE "repository"
  (
     repository_id INTEGER PRIMARY KEY,
     repositories  TEXT UNIQUE NOT NULL
  );

CREATE TABLE "repository_type"
  (
     repository_type_id INTEGER PRIMARY KEY,
     repository_types   TEXT NOT NULL,
     built_in           BOOLEAN DEFAULT 1,
     hidden             BOOLEAN DEFAULT 0
  );

CREATE TABLE roles_links
  (
     roles_links_id INTEGER PRIMARY KEY,
     role_type_id   INTEGER NOT NULL,
     person_id      INTEGER NOT NULL,
     event_id       INTEGER NOT NULL,
     FOREIGN KEY (role_type_id) REFERENCES role_type (role_type_id),
     FOREIGN KEY (person_id) REFERENCES person (person_id),
     FOREIGN KEY (event_id) REFERENCES event (event_id)
  );

CREATE TABLE "role_type"
  (
     role_type_id INTEGER PRIMARY KEY,
     role_types   TEXT UNIQUE NOT NULL,
     built_in     BOOLEAN NOT NULL DEFAULT 0,
     hidden       BOOLEAN NOT NULL DEFAULT 0
  );

CREATE TABLE "source_type"
  (
     source_type_id INTEGER PRIMARY KEY,
     source_types   TEXT NOT NULL,
     built_in       BOOLEAN DEFAULT 1,
     hidden         BOOLEAN DEFAULT 0
  );

CREATE TABLE to_do
  (
     to_do_id INTEGER PRIMARY KEY,
     to_dos   TEXT NOT NULL DEFAULT '',
     priority INTEGER
  );

CREATE TABLE transcription
  (
     transcription_id      INTEGER PRIMARY KEY,
     name_id               INTEGER,
     nested_place_id       INTEGER,
     transcription_type_id INTEGER,
     transcriptions        TEXT NOT NULL,
     FOREIGN KEY (name_id) REFERENCES NAME (name_id),
     FOREIGN KEY (nested_place_id) REFERENCES nested_place (nested_place_id),
     FOREIGN KEY (transcription_type_id) REFERENCES transcription_type (
     transcription_type_id)
  );

CREATE TABLE "transcription_type"
  (
     transcription_type_id INTEGER PRIMARY KEY,
     transcription_types   TEXT UNIQUE NOT NULL,
     romanized             BOOLEAN DEFAULT 0,
     phonetic              BOOLEAN DEFAULT 0,
     built_in              BOOLEAN NOT NULL DEFAULT 1,
     hidden                BOOLEAN NOT NULL DEFAULT 0
  );
