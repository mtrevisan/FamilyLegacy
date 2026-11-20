# Sources, media and citations

For what all the editors share (tabs, confidence, privacy, audit) see [The record editors](editors.md). For the commands of the Source menu see [Sources and media](sources-and-media.md).

## Source

Open it with **Source > New Source...** or **Source > Manage Sources...**, or from a *Sources* tab (*Create New...* or *Add Existing...*). **Tabs:** Properties, Research, Sources, Notes, Privacy, Audit. The only thing checked on OK is that no *Derived from* entry is this source or derives from it.

| Field | Meaning |
|---|---|
| **Title** | Required: the title, in one or more [names](editors-people.md#name). |
| **Author** | The person, agency or entity that created the record (author, compiler, transcriber, abstractor, editor, or a church, an agency, a private organization). Each entry is an [agent](#agent). |
| **Publisher** | Text. |
| **Date** | A [date field](dates.md). |
| **Place** | Where the source was made. |
| **Kind** | Editable. What the source **is**: civil registration, church register, census, deed, will / probate, court record, military record, school record, immigration, tax roll, directory, newspaper, periodical, book, cemetery record, correspondence, diary, oral, website, map, photograph, artwork, audio, video, manuscript, patent, identification, compilation (an index, transcription, abstract or compiled genealogy made from other sources), artifact. Hover over a value for its meaning; any other text is accepted. It is the *genre* of the source, not its physical form (that is the *Media Type*). The [Research Assistant](assistant.md) uses it to say what to look for in the source. |
| **Media Type** | Editable. The physical form of the source: audio, book, card, electronic, fiche, film, magazine, manuscript, map, newspaper, photo, tombstone, video. |
| **Identifiers** | How the source itself is identified: an ISBN, an ISSN, a DOI, a URL, a microfilm number... A source may have several. Each entry is an [identifier](#identifier). |
| **Repositories with Citations** | Where the source is kept: each entry is a repository with the [identifiers](#identifier) that say where the source is found in it (shelf mark, call number, classification number), a locator and remarks. See [Repository citation](#repository-citation). |
| **Media** | The media records (scans, photographs) of the source. |
| **Derived from (Sources with Citations)** | In the **Sources** tab. The sources this one was made from, when it is not an original: a transcription, an abstract, an index, a copy, a compilation, or a database taken from a register or a book. See [below](#sources-made-from-other-sources). |

### Sources made from other sources

An **original** source (the parish register itself, a deed, a tombstone) has nothing in *Derived from*. A source that is a transcription, an abstract, an index, a copy or a compilation of something else lists what it comes from there, so that the chain from the copy back to the original is written down.

- In the **Sources** tab, **Derived from** is a list of citations, like the **Sources** tab of any other record: *Create New...* takes an existing source or creates one, and for each entry you fill in the same fields as in any [source citation](#source-citation).
- The **Locator** says where in the original the content comes from (*page 45, entry 12*) and the **Extract** the passage; both are optional.
- The **Stance** is *supports* in the usual case. Choose *contradicts* when this source disagrees with its original (a mistaken transcription, an index that gives the wrong year): the disagreement is then recorded and shows in red in Provenance.
- Below the list, a **Derivations** tree shows the whole chain at a glance: the sources this one is made from, up to the originals, and the sources made from it. It appears only when at least **3** other sources are in the chain, above or below (with fewer, the list is enough), and it shows the state saved in the file, not the changes you are making. Each line gives the title, the identifier, the locator, and *(contradicts)* in red when the derived source disagrees with its original; *(circular derivation)* marks a chain that comes back on itself (Validate File reports it). Double-click a source to open it.
- A source can derive from more than one source (a compilation of several registers), and several sources can derive from the same one.
- It is about **where the content comes from**. A book that lists other works in a bibliography, or a note that mentions another source, does not derive from them: leave *Derived from* empty and write it in the notes.
- A source cannot derive from itself, directly or through other sources (a transcription of a copy of that transcription). The editor refuses it on OK, and [Validate File](tools.md#validate-file) reports it as an error if it is found in a file.
- Several sources derived from the same original are **not independent evidence** of what it says: two indexes of one register are one witness, not two. This is why it is worth writing the original down.
- *Derived from* is separate from the **Source Type** of a citation (*original* or *derived*, see [Source citation](#source-citation)): that one is your assessment of how the evidence was used in a record; this one links the source to the one it comes from. The two can be used together.
- The imports from GEDCOM and Gramps do not fill it in, as neither format has this link.

## Repository

An archive, a library, a parish office. Open it with **Source > New Repository...** or **Manage Repositories...**. **Tabs:** Properties, Sources (the sources kept here), Notes, Privacy, Audit.

| Field | Meaning |
|---|---|
| **Name** | The names of the repository; at least one is required. |
| **Type** | What kind of repository it is: archive, library, church, cemetery, website, collection, private collection, bookstore, government, museum, university, institution, historical society, genealogy society, funeral home, national library, national archives or prison library. Editable: you can also type your own word (lowercase letters, digits and underscores). |
| **Custodian** | Who looks after it, as an [agent](#agent). |
| **Place** | Where it is. |
| **Contacts** | The [contacts](#contact). |

## Repository citation

Every entry of the *Repositories with Citations* list of a source says where the source is kept, and how to find it there. **Tabs:** Properties, Notes.

| Field | Meaning |
|---|---|
| **Repository** | The repository that holds the source (required). |
| **Identifiers** | Where the source is found **in this repository**: a shelf mark, a call number, a Dewey or Library of Congress number. See [Identifier](#identifier). |
| **Locator** | The position **inside the source**: the page, the entry, the folio, the frame (*Page: 27*, *Entry: 45*, *Folio: 8r*, *Frame: 213*). It is not an identifier. |
| **Remarks** | Short free-text remarks. |

The list shows the repository followed, between brackets, by its identifiers.

## Identifier

How an item is identified. Four things are kept apart, and mixing them makes a source impossible to find or to tell from another one:

| What | Where it goes | Examples |
|---|---|---|
| **The kind of the source** (its genre) | *Kind* of the source | church register, census, book |
| **The form of the carrier** | *Media Type* of the source | book, film, fiche |
| **How the source itself is identified**: the same wherever the item is kept | *Identifiers* of the **source** | ISBN, ISSN, DOI, URL, FHL film number |
| **Where the source is found in one repository**: valid only there | *Identifiers* of the **repository citation** | shelf mark, call number, Dewey number, LCC |
| **Where the information is inside the source** | *Locator* (of the repository citation, or of a [source citation](#source-citation)) | Page 12, Entry 45, Folio 8r, Frame 213 |

An identifier has a **Scheme** and a **Value**, both required. The scheme is editable: choose one of the list, or type your own word, which is kept as written. The list offers first the schemes that suit what you are editing (the ones of a source itself, or the ones of a repository), and hovering over a value tells what it is.

| Scheme | What it is |
|---|---|
| **ISBN** | International Standard Book Number (ISBN-10 or ISBN-13). |
| **ISSN** | International Standard Serial Number, of a periodical. |
| **DOI** | Digital Object Identifier. |
| **URL** | Web address (RFC 3986). |
| **call number** | Shelf mark, archival reference or generic call number. |
| **Dewey**, **LCC**, **UDC**, **Colon**, **Garside** | The number of a library classification. |
| **FHL film**, **FHL digital folder**, **FHL Ancestral File** | The numbers of FamilySearch (the Family History Library). |
| **index card** | An index card or an entry of a card catalog. |

The editor tries to help: when you leave the *Value* with no scheme, a value that is surely one is recognized (an ISBN with a right check digit, an ISSN, a DOI, a URL). A value that fails the check of its scheme (the check digit of an ISBN or an ISSN, the syntax of a URL) is shown with a warning under the field, and it does not stop you from saving: a printed number can be wrong, and an old edition can predate the check. [Validate File](tools.md#validate-file) reports them as warnings, and the [Research Assistant](assistant.md) says which one to check. The other schemes, and the ones you type, are not checked.

Two sources with the same identifier (an ISBN written with or without hyphens, as ISBN-10 or ISBN-13; a DOI with or without its `https://doi.org/`) are probably the same source: the merge of two files takes it into account.

## Media

A record that points to a file. Open it with **Source > Manage Media...** (*New...* or *Edit...*), or from the *Media* list of a source. **Tabs:** Properties, Research, Sources (the sources that contain this media), Notes, Privacy, Audit.

| Field | Meaning |
|---|---|
| **URI** | The location of the file: right-click offers *Set* (to choose a file) and *Clear*. |
| **Kind** | image, document, audio or video. |
| **Mapping** | Editable. planar, spherical-equirectangular, spherical-UV, cubemap, cylindrical-equirectangular horizontal, cylindrical-equirectangular vertical. |
| **Description** | Free text. |

**Where Used...** lists every record that uses the media (as a media of a source, in a media part of an extract, as a preferred image or in the *Media* list of an individual, group, place, event or repository, and so on); double-click a line, or select it and press **Open**, to open that record's editor. It is disabled while the media is new. Deleting a media removes it from all those places, and merging two media makes them all point to the one that is kept.

URI and kind are required. The field always shows the location whole; how it is *shown* in the lists is a preference, see [Preferences](preferences.md#media-paths).

## Source citation

Every entry of a *Sources with Citations* list is a citation: it ties an assertion to a source. **Tabs:** Properties, Privacy.

| Field | Meaning |
|---|---|
| **Source** | The source cited. |
| **Locator** | Where in the source: volume, page, entry. The [Source Citation Assistant](assistant.md#the-source-citation-assistant) can build it. |
| **Extracts** | The parts of the source that you quote or point to: see [Extract](#extract). |
| **Stance** | *supports* or *contradicts*: how the citation relates to the assertion. |
| **Evidence** | The [evidence qualifiers](#evidence-qualifiers). |

The source and the stance are required. Every extract in the list must belong to the source cited: if you change the source, the editor refuses OK until you remove the extracts of the other source.

The **Extracts** list (right-click, or `Ins` and `Del`):

- **Create New...** opens the [extract](#extract) editor for a new extract of the source cited (choose the source first: the extract belongs to it, and its *Source* cannot be changed there).
- **Add Existing...** lists the extracts of the source cited that the citation does not reference yet, so that one transcription can be cited by several records.
- **Edit...** opens the extract. An extract is a record of its own: a change is seen by every citation that references it.
- **Remove** removes only the reference: the extract stays with its source.

The extracts created from a citation that you then cancel are discarded.

## Extract

What a source says, kept once and cited as many times as needed: a transcription, a translation, a summary or an excerpt, and the parts of the media where it is (a page, a region of an image, a segment of a recording). An extract is not evidence by itself: the citations that reference it are. Open it from the *Extracts* list of a [source citation](#source-citation). **Tabs:** Properties, Privacy, Audit.

| Field | Meaning |
|---|---|
| **Source** | Required: the source the extract belongs to. It is fixed when the extract is created from a citation, and it cannot be changed once a citation references the extract. |
| **Media Parts** | The media of the source where the passage is: an image with an optional crop, a page of a document (with an optional crop), or a segment of an audio or video recording. |
| **Text** | The text of the passage. |
| **Type** | How the text relates to the source: *verbatim* (exactly as written), *summarized*, *translated* or *normalized* (same language, regularized spelling, expanded abbreviations). |
| **Locale** | The language of the text. |
| **Remarks** | Short free-text remarks about the extract. |

At least one of media part and text is needed. A media part must be a media of the source: if it is not, on OK you are asked whether to add it to the *Media* of the source (answering no goes back to the editor).

Deleting a source (from **Manage Sources...**) deletes its extracts too, and removes the citations of the source and the references to its extracts. Files written before the extracts became records are converted when they are opened: each extract of a citation becomes an extract of the source cited (identical extracts of the same source become one).

## Evidence qualifiers

A citation (and a cultural norm) can say how good the evidence is, with three optional combos:

| Qualifier | Values |
|---|---|
| **Source Type** | *original*: the first recording of the information (the register itself). *derived*: a copy, a transcription, an abstract, an index of an original. |
| **Info Type** | *primary*: the informant had first-hand knowledge (a father giving the birth date of his child). *secondary*: second-hand knowledge (someone reporting a birth he did not witness). *undetermined*: the informant, or how they knew, is not known. It depends on the informant, not on whether the source is original. |
| **Evidence Type** | *direct*: the information states the assertion outright. *indirect*: it implies it (an age at death implying a birth year). *negative*: the assertion is inferred from an absence (no entry where one would be expected). |

**Research > Evidence Qualifiers...** reports how they are distributed in the file, see [Research](research.md#evidence-qualifiers).

## Place citation

The **Place** field of many records is a place citation: an **Original Text** (the place as the source writes it) and a **Confidence**, plus a **Sources** tab. The place itself is chosen from the field.

## Note

Open it from a **Notes** tab. **Tabs:** Properties, Sources, Privacy, Audit.

| Field | Meaning |
|---|---|
| **Title** | Optional. |
| **Text** | The note (required). Markdown is supported, with two special links, shown below. |
| **MIME Type** | blank, text/plain, text/html or text/markdown. |
| **Locale** | The language of the note. |
| **Translations** | Translations of the note, each with its text and an optional locale. |

The two special links of the note text:

```
[text](@<XREF:ID>@)         a reference to a record
[text](flef:confidential)   data to be treated as confidential
```

The second one only acts in the FLEF export, not on screen or in the reports: do not rely on it alone to keep something private. See [Privacy](privacy.md#the-confidential-marker-in-notes).

## Agent

Used for the author of a source and the custodian of a repository. The **Type** is *agent* (then you choose an individual or a group) or *text* (then you write the name as free text). The chosen kind must be filled in.

## Contact

A contact of a repository. **Tabs:** Properties, Privacy, Audit.

| Field | Meaning |
|---|---|
| **Value** | The address, number or link (required). |
| **Type** | blank, email, phone, mobile, fax, website, blog, social, postal, messaging. |
| **Name** | Names for the contact, each with variants. |
| **Note** | A remark. |
