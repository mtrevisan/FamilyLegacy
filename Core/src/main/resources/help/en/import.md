# Importing from GEDCOM and Gramps

**File > Import** converts a file made by another program into a new FLEF file. This page explains what you see, what is kept, what is converted with a loss, and what is dropped.

## Before you start

- The import replaces the current document. After you confirm, the changes of the open document are written to its file first, as with New and Open.
- The original file is never modified. The import never merges into the open document.
- There is no report at the end of the import: the data that has no counterpart is dropped silently. Some data that cannot be converted is kept as a **note** on the record instead (this page says where). After an import, look at the notes of the people, events and relationships to see what the conversion could not express.

## The steps

1. **File > Import** and choose **GEDCOM 5.5.x / 7.0...** or **Gramps XML 1.7.2...**. A file chooser opens (GEDCOM files `*.ged`; Gramps files `*.gramps` and `*.gpkg`).
2. A confirmation says that the import will convert the file into a new FLEF file that replaces the current document. Answer **Yes**.
3. A progress window follows the stages: reading, parsing, registering identifiers, parsing records, resolving family links, building the model.
4. A **Save** chooser asks where to put the new `.flef`. It starts in the folder of the imported file, with the same name and the `.flef` extension, and asks before overwriting an existing file. If you **cancel** here, the import is discarded and the current document is left as it was.
5. The file is written and then opened, exactly as with **File > Open**.

If the file cannot be read, an **Import Error** dialog says that the file is not a valid GEDCOM (or Gramps) document, with the reason.

## GEDCOM

### Versions and character sets

GEDCOM **5.5**, **5.5.1** and **7** are read; the version is taken from the header (a version starting with 7 is GEDCOM 7, anything else is read as 5.5.x).

The character set is detected in this order: GEDCOM 7 is always UTF-8; otherwise a byte-order mark decides, then an automatic detection on the beginning of the file, and only if that is inconclusive the `CHAR` line of the header (ANSI, IBM PC, Macintosh, Unicode, UTF-8, UTF-16 and a few Windows code pages are understood). **ANSEL**, the old GEDCOM character set, has its own decoder and is the fallback. If accented letters come out wrong, the detection guessed the character set wrongly; re-save the file in UTF-8 from the program that made it.

Lines that are empty or malformed are skipped, and `CONC` and `CONT` lines are joined.

### People

- **Names**: the given name, the family name, the prefix, the suffix and the nickname become parts of a personal name. The pieces written in the `GIVN`, `SURN`, `NPFX`, `NSFX`, `NICK` lines win over the text of the `NAME` line. A name with no parts is dropped. The name type is kept for *birth*, *maiden*, *married*, *immigrant* and *aka* (which becomes *alias*); a type longer than four words is lost. Phonetic and romanised forms become name variants, or a note.
- **Sex**: M is male, F is female, U and X are unknown (so X is lossy).
- **Events** (birth, christening, death, burial, cremation, adoption, baptism, confirmation, naturalization, emigration, immigration, census, probate, will, graduation, retirement, ordination, and the LDS ordinances): each becomes **one event** with a participation of the person, with the role *principal*. The age becomes a note on the participation. Christening, baptism, bar and bat mitzvah, blessing, adult christening, confirmation, first communion and ordination become custom event types of their own. A generic `EVEN` takes its type from its `TYPE` line.
- **Attributes** (caste, description, education, ID number, nationality, number of children, number of marriages, occupation, property, religion, residence, social security number, title, fact): each becomes an attribute record. Both *ID number* and *social security number* become *national ID*, which the application treats as confidential (see [Privacy](privacy.md)).
- **Aliases**: a pointer to another person creates an identity hypothesis; an alias written as text becomes a name of type *alias*.
- **Associations**: they become *associate* relationships, with the relation text as the role.

### Families

Each `FAM` becomes a **group of type family**, named after the surnames of the spouses, with a *group member* relationship for each spouse and each child (roles *husband*, *wife*, *child*). On top of that:

- When both spouses exist, a **spouse relationship** is created, even if there is no marriage event. It is *civil spouse* or *religious spouse* if the marriage type says so; otherwise it is the generic *spouse* **with a note** that asks you to decide if it is civil or religious ("TO BE REVISED"). The marriage date becomes its *Valid From*, and the divorce or annulment date its *Valid To*.
- Each child gets a parentage relationship to each parent. Its type comes from the `PEDI` value: *birth* (or missing or unknown) is *biological child*, *adopted* is *adoptive child*, *foster* is *foster child*, *sealing* is the generic *child*. A status of *challenged* or *disproven* sets the confidence to low, and *proven* to high.
- The family events become events with participations for the group and for each spouse.

A `FAM` with no identifier is skipped.

### Sources, repositories, notes and media

- **Sources**: the title, abbreviation, author and publisher are kept. The events a source records are gathered in a note. A source cited with free text creates a source whose title is that text; identical ones are merged.
- **Citations**: the page becomes the **locator**; the quoted text becomes an **extract** of type *verbatim*; every citation has the stance *supports*. The quality of the data (`QUAY`) is converted into the *evidence qualifiers*: 3 is *primary* and *direct*, 2 is *secondary*, and 0 and 1 are both *undetermined* (they are not told apart).
- **Repositories**: name, address, telephone, e-mail, fax and web address (as contacts), with the call number as the locator of the citation.
- **Notes**: they are kept; a shared note is copied wherever it is referenced.
- **Media**: only the **first file** of each object is kept, as its location, **written exactly as in the GEDCOM**. No file is copied or checked, so if the paths do not exist on your computer, the media will not open. The other files become notes. A media object that has no file is lost. A media link on a person becomes a citation of a source made of that media, and the first marked as primary becomes the preferred image, with its crop.

### Places

A place written as `San Martino, Treviso, Veneto` becomes one place for each level, each contained in the next (*administrative part of*), and places with the same name but different containers stay separate. The level names declared in `PLAC.FORM` are **not** read, so the places get no type. Coordinates become the coordinates of the most specific place only.

### Dates

- Exact dates, months and years are read, with English month names only; `BC` is understood, and so are dual years like `1750/51`.
- `ABT`, `CAL` and `EST` become an **approximate** date (basis *stated*, *calculated* and *unspecified*). `BEF` and `AFT` become a bounded date, `BET ... AND ...` a bounded date with both limits, `FROM ... TO ...` a spanning date. `INT` keeps its phrase as the original text.
- The calendar escapes for Gregorian, Julian, Hebrew and French Republican are kept. Without one, a year before 1582 is taken as Julian and a later one as Gregorian. Roman and unknown calendars cannot be converted.
- A date that cannot be understood is **not lost**: it is kept as a **note titled "Date"** with the original text.

### Privacy and the change date

`RESN` becomes the **privacy level** of the record: *privacy* and *private* become *restricted*, *confidential* stays *confidential*; *locked* is dropped. The date of the last change becomes the audit date. There is no automatic marking of living people at import; see [Privacy](privacy.md).

### What is dropped

These have no place in the result and are dropped without notice: the submitter and submission records, `ANCI`, `DESI`, `RFN`, `AFN`, `REFN`, `RIN`, `UID`, `EXID`, the address and the contact lines directly under a person, and **every tag that starts with an underscore** (the private tags of each program), except the few that mark the preferred image, the crop, and the e-mail and web address of a repository. Of the header, only the program that made the file, its date, the copyright, the submitter and the note are kept.

GEDCOM 7 is first rewritten into the older syntax: its shared notes become ordinary notes, its calendar keywords become escapes, and a link to a record that does not exist (`@VOID@`) becomes the citation of an unrecorded source, or a note, or is dropped.

## Gramps XML

The file can be a `.gramps` file (compressed or not) or a `.gpkg` package. From a package, **only the data is taken**: the media files inside it are not extracted.

- **People**: the first name is the primary one and the others are additional names (*Birth Name* is *birth*, *Also Known As* is *alias*, *Married Name* is *married*). Surname derivations (patronymic, matronymic, location, occupation, feudal) are kept as the type of the part. Addresses become *residence* attributes, and the URLs become notes. Associations become *associate* relationships.
- **Families**: a group of type family, with the members and the parentage relationships (*Birth* is *biological child*, *Adopted*, *Stepchild*, *Foster*; anything else is the generic *child* with a note). A spouse relationship is created when both parents exist: *Civil Union* is *civil spouse*, *Unmarried* is *cohabiting partner*, otherwise the generic *spouse*. Unlike GEDCOM, no "to be revised" note is added.
- **Events**: one event with a participation for every person and family that refers to it. The roles are converted (Primary, Family, Bride and Groom become *principal*; Clergy and Celebrant become *officiant*; Godparent becomes *sponsor*). A personal event that is really an attribute (occupation, residence, education, religion...) becomes an **attribute** instead.
- **Places**: all the names (with language and validity dates), the type, the coordinates, and the hierarchy with its date ranges.
- **Sources and citations**: title, author, publisher, repositories; in citations, the page is the locator and the confidence (0 to 4) is converted like the GEDCOM quality: 3 and 4 *primary* and *direct*, 2 *secondary*, 0 and 1 *undetermined*.
- **Media**: relative paths are completed with the media path of the file; the files are **not copied**. The regions of an image are kept only as a note, not as a crop.
- **Notes**: bold, italic and links become Markdown; other styles are lost. Notes that no record refers to are not imported.
- **Tags**: the names of the tags become a note titled *Tags*; their colours and priorities are lost.
- **Dates**: *before*, *after*, *about*, ranges and spans are converted, and so are the Gregorian, Julian, Hebrew and French Republican calendars. The Islamic, Persian, Swedish and other calendars are not: such a date, and a date written as text, becomes a note titled *Date*.
- **Privacy**: the private flag of Gramps becomes the level *restricted* on people, families, events, places, repositories, sources, media, attributes, addresses and the other records that carry the flag. It is **not** read on citations and names.
- Not converted: bookmarks, the name formats, the colours of the tags, and anything not listed here.

## After the import

1. Save the file under the name you want (the save chooser already did it).
2. Run [Tools > Validate File](tools.md#validate-file) and open the [Research Assistant](assistant.md): they show records that need care.
3. Look at the notes titled *Date*, *TO BE REVISED* (GEDCOM spouses) and *Additional file*, and at the media paths, which are as in the original file.
4. Check the privacy of living people: it is decided when you look at the data, not at import (see [Privacy](privacy.md#living-people)).
