# Sources, media and citations

For what all the editors share (tabs, confidence, privacy, audit) see [The record editors](editors.md). For the commands of the Source menu see [Sources and media](sources-and-media.md).

## Source

Open it with **Source > New Source...** or **Source > Manage Sources...**, or from a *Sources* tab (*Create New...* or *Add Existing...*). **Tabs:** Properties, Research, Sources, Notes, Privacy, Audit. Nothing is checked on Save.

| Field | Meaning |
|---|---|
| **Title** | The title, in one or more [names](editors-people.md#name). |
| **Author** | The person, agency or entity that created the record (author, compiler, transcriber, abstractor, editor, or a church, an agency, a private organisation). Each entry is an [agent](#agent). |
| **Publisher** | Text. |
| **Date** | A [date field](dates.md). |
| **Place** | Where the source was made. |
| **Media Type** | Editable. audio, book, card, electronic, fiche, film, magazine, manuscript, map, newspaper, photo, tombstone, video. |
| **Repositories with Citations** | Where the source is kept: each entry is a repository with a locator and remarks. |
| **Media** | The media records (scans, photographs) of the source. |

## Repository

An archive, a library, a parish office. Open it with **Source > New Repository...** or **Manage Repositories...**. **Tabs:** Properties, Sources (the sources kept here), Notes, Privacy, Audit.

| Field | Meaning |
|---|---|
| **Name** | The names of the repository; at least one is required. |
| **Custodian** | Who looks after it, as an [agent](#agent). |
| **Place** | Where it is. |
| **Contacts** | The [contacts](#contact). |

## Media

A record that points to a file. Open it with **Source > Manage Media...** (*New...* or *Edit...*), or from the *Media* list of a source. **Tabs:** Properties, Research, Sources (the sources that contain this media), Notes, Privacy, Audit.

| Field | Meaning |
|---|---|
| **URI** | The location of the file: right-click offers *Set* (to choose a file) and *Clear*. |
| **Kind** | image, document, audio or video. |
| **Mapping** | Editable. planar, spherical-equirectangular, spherical-UV, cubemap, cylindrical-equirectangular horizontal, cylindrical-equirectangular vertical. |
| **Description** | Free text. |

URI and kind are required. The field always shows the location whole; how it is *shown* in the lists is a preference, see [Preferences](preferences.md#media-paths).

## Source citation

Every entry of a *Sources with Citations* list is a citation: it ties an assertion to a source. **Tabs:** Properties, Privacy.

| Field | Meaning |
|---|---|
| **Source** | The source cited. |
| **Locator** | Where in the source: volume, page, entry. The [Source Citation Assistant](assistant.md#the-source-citation-assistant) can build it. |
| **Extracts** | The parts of the source that you quote or point to: see below. |
| **Stance** | *supports* or *contradicts*: how the citation relates to the assertion. |
| **Evidence** | The [evidence qualifiers](#evidence-qualifiers). |

The source and the stance are required.

**Extract.** An extract has a **media part** (a media record, with an optional crop), a **text**, a **type** (*verbatim*, *summarized*, *translated* or *normalized*), a **locale** and a **remark**. At least one of media part and text is needed.

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
