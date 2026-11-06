# Reports

The **Reports** menu produces documents from the data of the file. All twelve reports open the same dialog, and each writes one file for every format you tick.

## The reports

| Report | Available when | Starts from |
|---|---|---|
| **Ancestor Report...** | an individual is selected | the selected individual |
| **Descendant Report...** | an individual is selected | the selected individual |
| **Group Sheet...** | a group is selected | the selected group |
| **Individual Summary...** | an individual is selected | the selected individual |
| **Relationship Report...** | an individual is selected | the selected individual |
| **Place Report...** | the file has a place | a place: the selected one, or the one you pick from the list |
| **Source Report...** | the file has a source | a source: the selected one, or the one you pick from the list |
| **Research Question Report...** | the file has a research question | a question: the selected one, or the one you pick from the list |
| **Bibliography...** | the file has a source or a media record, and a record is selected | the selected record |
| **Full Bibliography...** | the file has a source | the whole file: every source |
| **Research Progress...** | the file has a research question or a task, and a record is selected | the selected record |
| **Family Book...** | the file has an individual | the whole file: everybody |

Select the person or group you want in the tree **before** you open the report. Places, sources and research questions cannot be selected in the tree: for the *Place*, *Source* and *Research Question* reports a list opens first, to pick the record (if a record of that kind is already selected, it is used without asking). The first line of the dialog, *Target Subject*, tells which record it will use. Every report is about a record, so the commands stay greyed out until one is selected (or, for the three above, until the file has one; the *Full Bibliography* and the *Family Book* are about the whole file and need no selection: the first line of their dialog says so), and **Generate...** refuses to write a report without a subject. The *Bibliography* lists the sources cited by that record (by its events, attributes and relationships), not those of the whole file.

## The dialog

From the top:

1. **Target Subject**: the record the report is about.
2. **Output Formats**: *Plain Text (.txt)*, *Markdown (.md)* (ticked by default), *HTML (.html)*, *Word (.docx)*, *PDF (.pdf)*, *EPUB (.epub)* and *Static Website (folder)*. You can tick several. Word, PDF, EPUB and Static Website are optional components of the installation (see [The formats](#the-formats)): when one is missing, its box is greyed out and a tooltip says which file is not installed.
3. **Language** of the document, see [Language](#language). It starts as the language of the application, until you choose another.
4. The **options**, see below.
5. **Restore Defaults**, **Cancel** (`Esc`) and **Generate...**.

**The dialog remembers your choices.** When you press **Generate...** and choose a folder, the dialog keeps the formats you ticked, the language, the folder, and the options of that kind of report (the *Ancestor Report* and the *Descendant Report* each have their own), and offers them again next time, also after the application is closed. **Restore Defaults** brings back the default options, the language of the application and the default format (Markdown). The **privacy policy** is the exception: it is never remembered and **always starts at Hide Confidential**, so that a choice made for a private report is not carried by mistake to one you will share. Hover over an option to read a short description of it.

**The dialog shows only the options that the report uses.** An option that would change nothing in that report is not listed (for example, the ancestry options in the *Group Sheet*, the group options in an *Individual Summary*, or the timeline in an *Ancestor Report*), and a heading with no options left disappears. An option that the report always applies is shown **ticked and greyed out**, with a tooltip saying so (for example, *Direct relations* in the *Relationship Report*). What each report does with each option is in [What each report contains](#what-each-report-contains).

### Where the files go

**Generate...** asks for a **folder** (there is no file-name prompt). The files are written there as `ancestor_report`, `descendant_report`, `group_report`, `individual_report`, `relationship_report`, `place_report`, `source_report`, `research_question_report`, `bibliography_report`, `full_bibliography_report`, `family_book_report` or `research_progress_report`, with the extension of the format (the static website is a **folder**, named like `individual_report_site`). **If a file with the same name is already there, you are asked before it is overwritten.** A progress window with a **Cancel** button follows, and at the end a message lists the files written, with **Open Directory** to see them in the file manager. The reports are not opened for you.

Images are not copied next to the file in every format: Markdown and HTML refer to the original media files (with paths relative to the folder, when possible), so keep the media where they are; Word, PDF and EPUB embed the images when the report is made; the static website copies them into the `images` folder inside its own folder.

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
- **Order of persons**, for the *Family Book* only: *By family name* (default; then by the other names; whoever has no name comes last) or *By birth date* (the oldest first; whoever has no birth year comes last, in alphabetical order).
- **Generations**, for the ancestor and descendant reports only, decides how far to follow the line: *All* (default), or from 1 to 20 generations (the parents, or the children, are the first). The person the report is about is not counted, and the tree, the list of persons and the collaterals stop there.

**Content sections**: **Notes**, **Sources**, **Media**, **Historic events**, **Repositories** (on), and **Historic context & research**, **Cultural norms**, **Evidence qualifiers** (off).

**Group report**: **Groups and memberships** (on: the *Group memberships* list in the relationships of a person), and, for group roots and for the *Family Book*, **Show group members**, **Show subgroups** and **Show group attributes** (on). In the *Family Book*, **Groups and memberships** also decides whether the groups have a page.

**Diagnostics** (all off): **Full event participants** (adds *Other participants* to each event), **Coverage report** (a table of how complete and how sourced each record is, and the number of records without a source citation) and **Chronological timeline** (a table of the events of the root, with the related historic events; for individual, group and place roots).

**Indexes**: **Index of individuals** and **Index of places** (on). In the *Family Book*, **Index of individuals** also adds the index of the groups.

**Style**: **Include descriptions** (on): in the Media section, adds the location, description and mapping of each media item. **Include pictures** (on): shows the preferred image of a person or group and the images of the Media section; with it off, the media are listed as links to their files.

## What each report contains

**Individual Summary** is the full report on one person. In order: the *Document information* (if on); the **Introduction** (a line saying who it is about, the lifespan, and the number of events, sources and relatives); the **Life of** the person, with the preferred image, the *Personal data*, the *Life events* (one heading each), the *Attributes*, the *Relationships* (with the *Group memberships*), the *Notes*, the *Citations* and the *Audit trail* if on; then the paternal and maternal ancestry, the descendants and the relations if on; the timeline; the historic context and research (with identity hypotheses and conclusions); the cultural norms; the historic events; the repositories; the sources; the media; the places; the coverage report; and the indexes.

**Group Sheet** has the *Introduction* and the **History of** the group: the group data, the members (a table), the attributes, the groups it belongs to and contains, the events involving the group, the notes and citations; then the same closing sections as above.

**Place Report** is about one place: the *Place:* section with its names (with type, language and variants), type and coordinates, the **historical jurisdictions** (the places that contained it and the ones it contained, with their dates), the **contained places**, the **events at this place** and the events in which the place is a participant, the attributes recorded at the place, and its notes, citations and audit trail; then the common closing sections (the sources, media, places, and so on, as the options say). It has no *Introduction*, and no ancestry, descendants or group options.

**Source Report** is about one source: the *Source:* section with its title, author, publisher, date, place and type of medium, the **repository** that holds it, its **documents**, and **Cited by**: every citation that refers to the source, grouped by the record that makes it; then notes, evidence, privacy and audit trail, and the common closing sections.

**Research Question Report** is about one question: the *Research question:* section with the question, its status, conclusion and confidence, and then its **targets**, **activities**, **tasks** and **conclusions**, each with the detail of the *Research* section of the other reports; then the common closing sections. It is the report to hand to somebody who works with you on the question.

**Full Bibliography** is the bibliography of the whole file (the *Bibliography* above is the one of a record): the title, how many sources there are, a **table of every source** (title, author, date and how many times it is cited), and then an entry for each source, in alphabetical order, with the same content as the *Source Report*: author, publisher, date and place, the repositories that hold it, its documents and **Cited by** (every record that cites it, with the locator). A source that the privacy policy hides is left out. Only the options that make sense are shown: the document header, the privacy details, the audit trail, notes, sources (the citations of the sources themselves) and evidence.

**Family Book** is the document about everybody in the file: the *Document information* (if on), the title, how many individuals there are, the **Index of individuals** (if on) and, after it, the **Index of groups** (the same option decides both, and it is there only if the book has groups), then, for each person, on a new page, the introduction and the life story, and at the end what they all cite together: the **Repositories**, **Sources** and **Media** (if on), the **Places** and the **Index of places** (if on). The persons are in the *Order of persons* you choose, and the ones the privacy policy hides are left out. After the persons, if **Groups and memberships** is on (it is by default), there is a page for each **group**, in alphabetical order, as in the *Group Sheet*: the introduction and the *History of* the group, with its members, subgroups and attributes as the *Show group...* options say; the sources, media and places of the groups are in the closing sections too. For a large file the book is long, and takes a while to write.

**Ancestor Report** and **Descendant Report** have a different structure:

1. the *Document information* and the title (*Ancestors of ...*, *Descendants of ...*);
2. how many individuals the report covers;
3. the **Ancestor tree** (or **Descendant tree**), drawn in text, direct line only;
4. the **Collateral relatives**, if *Collaterals* is not *none*, each with its kinship and path;
5. the **Index of individuals**, if on;
6. then, for each person, on a new page, the introduction and the life story;
7. then, once for all the persons, what they cite together: the **Repositories** and the **Sources** (if on), the **Media** (if on), the **Places** and the **Index of places** (if on).

The per-person pages contain only what is written inside the life story (notes and citations, with the sources written out in the Sources section at the end). The timeline, the coverage report, the historic context and research, the cultural norms and the historic events are not part of these two reports, and the options about ancestry, descendants and relations do not apply to them: the dialog does not show these options.

**Bibliography** and **Research Progress** force some options, and the dialog shows them as such (the bibliography always has the sources, evidence qualifiers, repositories and index of places, and leaves out the narrative and the context, which are not shown; the research progress always has the context and research, the coverage report, the sources, the evidence and the indexes, and leaves out the ancestry, descendants, relations, media and the index of places, which are not shown). Both keep your choices for *Notes*, and for the other options they do not force.

### Things to know about these reports

- **The Relationship Report** is the Individual Summary with the ancestry and the descendants switched off, so that it concentrates on the **direct and indirect relations** of the person.
- **Bibliography** and **Research Progress** work on the **selected record**, not on the whole file: for an individual, the bibliography lists the sources cited by that person, their events, attributes and relationships. With an individual selected, the document also begins with that person's life story.

## Language

The **Language** list offers 46 languages, each in its own name. The texts that belong to the report (the section titles, the labels, the narrative sentences, the dates and the numbers) are translated for these 21: English (also UK and US), Arabic, Bengali, German, Spanish, Persian, French, Hindi, Indonesian, Italian, Japanese, Korean, Portuguese, Russian, Swahili, Thai, Turkish, Urdu, Vietnamese and simplified Chinese. The other languages in the list have no translation yet, and the report is probably written in English.

The **kinship terms** that follow a name (*uncle*, *grandmother*, *first cousin*, ...) are in the language of the report, with the form for the sex of the person, in **Italian, German, French, Spanish and Portuguese**. In the other languages they are written in English, even if the rest of the report is translated.

What you wrote in the records (names, notes, titles, places) is never translated.

The titles *Family Book* and *Bibliography* of the two whole-file reports are translated in English and Italian only so far: in the other languages they are written in English.

## The formats

Plain text, Markdown and HTML are always available. **Word, PDF, EPUB and Static Website are plugins**: they are the files `report-docx.jar`, `report-pdf.jar`, `report-epub.jar` and `report-site.jar` in the `plugins` folder of the installation. If one is not there, the format is still listed but greyed out, with a tooltip like *Not available: the plugin report-pdf.jar is not installed in the plugins folder*; copying the file there makes it available the next time the application starts.

- **Markdown**: UTF-8. The images are `![caption](path)`. Headings with a record identifier get an anchor, and the internal links work.
- **Plain text**: titles underlined, bullets as `-`, tables as rows separated by `|`, images as `[image: path]`. The bold, italic and code marks are removed.
- **HTML**: one file with its style inside. The images are **linked**, not embedded. The internal links work. The page is declared as English whatever the language.
- **Word**: the headings use the Word heading styles (so the navigation pane works), the tables have a shaded header, the images are embedded and scaled to fit, and the trees are in a monospaced font.
- **PDF**: A4, with a running title and page numbers, and the images embedded. It uses the standard PDF fonts, which are meant for Latin text: a report in Arabic, Hebrew, Cyrillic, Greek, Indic, Thai or Chinese or Japanese or Korean may show missing letters in the PDF. Use Word, EPUB or HTML for those languages.
- **EPUB**: an e-book (EPUB 3) with a **chapter for each first-level heading** and a table of contents, to read on an e-reader or in a reading app. The images are embedded inside the book, the internal links jump between chapters, a page break is ignored (the text reflows) and the book carries the language of the report.
- **Static website**: a **folder** with a home page (the title page and the contents), **one page for each first-level heading**, a navigation list on every page, links to the previous and next page, and the images in an `images` folder. It needs no server and no script: open `index.html` from the disk, or publish the folder as it is. The internal links jump between pages, and the language and its direction (right to left, where it applies) are those of the report. If the folder already exists, the pages and images of the earlier report are replaced and anything else in it is left alone.

## Other output

- **Tools > Statistics...** and **Tools > Consanguinity Report...** show their results on screen; see [Tools](tools.md) and [Kinship and consanguinity](kinship.md).
- **File > Backup...** saves a complete copy of the file; see [Files](files.md#backup-and-restore).
