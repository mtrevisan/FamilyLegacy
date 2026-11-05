# Reports

The **Reports** menu produces documents from the data of the file. All seven reports open the same dialog, and each writes one file for every format you tick.

## The reports

| Report | Available when | Starts from |
|---|---|---|
| **Ancestor Report...** | an individual is selected | the selected individual |
| **Descendant Report...** | an individual is selected | the selected individual |
| **Group Sheet...** | a group is selected | the selected group |
| **Individual Summary...** | an individual is selected | the selected individual |
| **Relationship Report...** | an individual is selected | the selected individual |
| **Bibliography...** | the file has a source or a media record, and a record is selected | the selected record |
| **Research Progress...** | the file has a research question or a task, and a record is selected | the selected record |

Select the person or group you want in the tree **before** you open the report. The first line of the dialog, *Target Subject*, tells which record it will use. Every report is about a record, so the commands stay greyed out until one is selected, and **Generate...** refuses to write a report without a subject. The *Bibliography* lists the sources cited by that record (by its events, attributes and relationships), not those of the whole file.

## The dialog

From the top:

1. **Target Subject**: the record the report is about.
2. **Output Formats**: *Plain Text (.txt)*, *Markdown (.md)* (ticked by default), *HTML (.html)*, *Word (.docx)* and *PDF (.pdf)*. You can tick several.
3. **Language** of the document, see [Language](#language).
4. The **options**, see below.
5. **Cancel** (`Esc`) and **Generate...**.

The options start from their default values every time: nothing is remembered between runs.

### Where the files go

**Generate...** asks for a **folder** (there is no file-name prompt). The files are written there as `ancestor_report`, `descendant_report`, `group_report`, `individual_report`, `relationship_report`, `bibliography_report` or `research_progress_report`, with the extension of the format. **If a file with the same name is already there, you are asked before it is overwritten.** A progress window with a **Cancel** button follows, and at the end a message lists the files written, with **Open Directory** to see them in the file manager. The reports are not opened for you.

Images are never copied into that folder: Markdown and HTML refer to the original media files (with paths relative to the folder, when possible), so keep the media where they are; Word and PDF embed the images when the report is made.

## The options

**Privacy**

- **Privacy policy**: *Show All*, *Hide Confidential* or *Hide Restricted and Confidential*, as in **View > Privacy**. It is independent of that menu and **always starts at Hide Confidential**. See [Privacy](privacy.md#reports).
- **Show privacy details**: under each record that has a privacy setting, prints its level, reason and expiry.
- **Audit trail**: adds the creation and update comments of each record.
- **Document header**: adds a *Document information* section, if the file has a header.

**Narrative**

- **Introduction**, **Descendants**, **Paternal ancestry**, **Maternal ancestry**, **Direct relations** (on by default) and **Indirect relations** (off).
- **Collaterals** decides who is included besides the direct line, for the ancestor and descendant reports only:
  - *none* (default): the direct line only.
  - *spouses*: plus the spouses.
  - *siblings*: for ancestors, plus the siblings of the ancestors (uncles and aunts) and their spouses; for descendants, plus step-children.
  - *cousins*: plus the descendants of those siblings (or of the step-children).
  - *extended*: everyone connected by any documented relationship, associates included.

**Content sections**: **Notes**, **Sources**, **Media**, **Historic events**, **Repositories** (on), and **Historic context & research**, **Cultural norms**, **Evidence qualifiers** (off).

**Group report**: **Groups and memberships** (on: the *Group memberships* list in the relationships of a person), and, for group roots, **Show group members**, **Show subgroups** and **Show group attributes** (on).

**Diagnostics** (all off): **Full event participants** (adds *Other participants* to each event), **Coverage report** (a table of how complete and how sourced each record is, and the number of records without a source citation) and **Chronological timeline** (a table of the events of the root, with the related historic events; for individual, group and place roots).

**Indexes**: **Index of individuals** and **Index of places** (on).

**Style**: **Include descriptions** (on): in the Media section, adds the location, description and mapping of each media item. **Include pictures** (on): shows the preferred image of a person or group and the images of the Media section; with it off, the media are listed as links to their files.

There is no option for how many generations to include: the ancestors and the descendants are followed all the way.

## What each report contains

**Individual Summary** is the full report on one person. In order: the *Document information* (if on); the **Introduction** (a line saying who it is about, the lifespan, and the number of events, sources and relatives); the **Life of** the person, with the preferred image, the *Personal data*, the *Life events* (one heading each), the *Attributes*, the *Relationships* (with the *Group memberships*), the *Notes*, the *Citations* and the *Audit trail* if on; then the paternal and maternal ancestry, the descendants and the relations if on; the timeline; the historic context and research (with identity hypotheses and conclusions); the cultural norms; the historic events; the repositories; the sources; the media; the places; the coverage report; and the indexes.

**Group Sheet** has the *Introduction* and the **History of** the group: the group data, the members (a table), the attributes, the groups it belongs to and contains, the events involving the group, the notes and citations; then the same closing sections as above.

**Ancestor Report** and **Descendant Report** have a different structure:

1. the *Document information* and the title (*Ancestors of ...*, *Descendants of ...*);
2. how many individuals the report covers;
3. the **Ancestor tree** (or **Descendant tree**), drawn in text, direct line only;
4. the **Collateral relatives**, if *Collaterals* is not *none*, each with its kinship and path;
5. the **Index of individuals**, if on;
6. then, for each person, on a new page, the introduction and the life story.

The per-person pages contain only what is written inside the life story (notes and citations). They do not repeat the sources, media, places, repositories, coverage report, timeline or context, and the options about ancestry, descendants and relations do not apply to them.

**Bibliography** and **Research Progress** force some options (the bibliography turns on the sources, evidence qualifiers and repositories and the index of places, and turns off the narrative and the context; the research progress turns on the context and research, the coverage report and the indexes, and turns off the ancestry, descendants, relations, media and the index of places). Both keep your choices for *Notes*, and the other options they do not force.

### Things to know about these reports

- **The Relationship Report** is the Individual Summary with the ancestry and the descendants switched off, so that it concentrates on the **direct and indirect relations** of the person.
- **Bibliography** and **Research Progress** work on the **selected record**, not on the whole file: for an individual, the bibliography lists the sources cited by that person, their events, attributes and relationships. With an individual selected, the document also begins with that person's life story.

## Language

The **Language** list offers 46 languages, each in its own name. The texts that belong to the report (the section titles, the labels, the narrative sentences, the dates and the numbers) are translated for these 21: English (also UK and US), Arabic, Bengali, German, Spanish, Persian, French, Hindi, Indonesian, Italian, Japanese, Korean, Portuguese, Russian, Swahili, Thai, Turkish, Urdu, Vietnamese and simplified Chinese. The other languages in the list have no translation yet, and the report is probably written in English.

The **kinship terms** that follow a name (*uncle*, *grandmother*, *first cousin*, ...) are in the language of the report, with the form for the sex of the person, in **Italian, German, French, Spanish and Portuguese**. In the other languages they are written in English, even if the rest of the report is translated.

What you wrote in the records (names, notes, titles, places) is never translated.

## The formats

- **Markdown**: UTF-8. The images are `![caption](path)`. Headings with a record identifier get an anchor, and the internal links work.
- **Plain text**: titles underlined, bullets as `-`, tables as rows separated by `|`, images as `[image: path]`. The bold, italic and code marks are removed.
- **HTML**: one file with its style inside. The images are **linked**, not embedded. The internal links work. The page is declared as English whatever the language.
- **Word**: the headings use the Word heading styles (so the navigation pane works), the tables have a shaded header, the images are embedded and scaled to fit, and the trees are in a monospaced font.
- **PDF**: A4, with a running title and page numbers, and the images embedded. It uses the standard PDF fonts, which are meant for Latin text: a report in Arabic, Hebrew, Cyrillic, Greek, Indic, Thai or Chinese or Japanese or Korean may show missing letters in the PDF. Use Word or HTML for those languages.

## Other output

- **Tools > Statistics...** and **Tools > Consanguinity Report...** show their results on screen; see [Tools](tools.md) and [Kinship and consanguinity](kinship.md).
- **File > Backup...** saves a complete copy of the file; see [Files](files.md#backup-and-restore).
