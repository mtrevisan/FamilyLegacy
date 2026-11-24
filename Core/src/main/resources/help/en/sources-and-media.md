# Sources and media

## Sources

A **source** is where information was found: a parish register, a census, a letter, a book. The **Source** menu has:

- **Manage Sources...** opens the list of sources, where you can search, create, edit and delete them (see [Manage dialogs](searching.md#manage-dialogs)). Deleting a source deletes its [extracts](editors-sources.md#extract) as well, and removes its citations from the records. Each line shows the author, the type and how many citations the source has: a source with no citation is one to look at. It is available when the file has sources.
- **New Source...** creates a source.
- **Source Workspace...** is the view centred on one source (it opens on the source selected, if any). On the left the sources are listed **by repository**, **by kind of repository** (archive, library, private collection...) or **by collection / record group**, that is by the shelf mark or call number written in the repository citation of the source (a source kept in two repositories is in both groups; one with no repository is under *(none)*). On the right, for the selected source, one tab for each thing that depends on it: the **citations** (the cited record, locator and stance, with the individuals each one is about), the **individuals**, **families** (partnerships) and **events** it backs, its **extracts** and the **media** it refers to. Selecting a citation lists below the individuals it is about, so you can walk *source -> citation -> person*; selecting an extract lists below what it **supports** and what it **contradicts** (the assertions whose citations reference it, with the individuals they are about), so you can walk *extract -> assertion -> person*, and *Explain This* on an extract opens its source; a double-click opens the record, and the lists are read again afterwards. It never changes the file by itself and is available when the file has sources.

## Repositories

A **repository** is the place that keeps a source: an archive, a library, a parish office.

- **Manage Repositories...** lists them, in the same way (see [Manage dialogs](searching.md#manage-dialogs)), with the place and the number of sources each one keeps.
- **New Repository...** creates one.

- **What a register covers.** There is no field for the years or the volumes that a source covers: write it in the **note** of the source, of the repository, or in the **remark** of the repository citation, in plain words (*Baptisms 1780-1820; volumes for 1794-1796 lost.*, *Entries 1-56 missing.*). Real archives have gaps, lost volumes, partial runs and unindexed registers, which a from-to range cannot say; a note can, and it is found by the search with *Search in notes and citations*.

## Media

A **media** record points to a file: a scan, a photograph, a document.

- **Manage Media...** lists the media with their location, description, the number of records that reference them and the number of **people tagged** in them (see [Regions of an image](editors-sources.md#regions-of-an-image)), and shows a preview of the selected image. **View...** opens the selected image at full size with its regions; **Media of...** shows only the media of one individual or group. You can search, create, edit and delete media. Only local files are previewed, to keep the dialog responsive.
- **SVG images** are shown, but for safety they never run scripts or load anything outside the file, and an SVG larger than 5 MB or declaring more than 4096 pixels per side is refused.
- **View...** in the media editor, or a double click on a thumbnail of the *Images* strip, opens the image at full size with its **regions** (who is in each part of a photograph): hover for the names, click to open the person. See [Regions of an image](editors-sources.md#regions-of-an-image).
- A media can have a **date**: when the photograph, recording, scan or document was created or captured. It matters when that date has historical relevance, and it is edited like any other date.
- The location can be a path on your disk, a `file:` address or a web address. How the location is *shown* is a preference: see [Preferences](preferences.md#media-paths).

## Citations

A **citation** ties a statement to its source and says where in the source it is found (the *locator*: volume, page, entry).

- **Source > Add Citation...** adds a citation to a record: you choose the record (an individual, group, event, place or relationship, shown by name), then the source, and fill in the locator and the stance (*supports* or *contradicts*). The citation is written in the record when you press *Continue*, with that locator and stance; you are then offered to open the record, to complete it with an extract or the evidence qualifiers.
- Citations can also be added from the **Sources** tab of any editor, where **Add Citation** takes an existing source or creates one, and where you can add an extract with the text of the source.
- The [Source Citation Assistant](assistant.md#the-source-citation-assistant) turns an archival reference such as *vol. 3, p. 45, n. 12* into a locator.
- How reliable the evidence is (original or derived source, primary or secondary information) is recorded with *evidence qualifiers*, see [Research](research.md#evidence-qualifiers).
- A source that is a transcription, an abstract, an index or a copy of another source says so in its **Derived from** list (**Sources** tab of the source), with where in the original the content comes from. See [Sources made from other sources](editors-sources.md#sources-made-from-other-sources).

## Checking how sources are used

**Source > Citation Analysis...** shows, in three tabs:

- **Unused sources**: sources that no record cites (a source that another source is *derived from* counts as cited);
- **Unused documents**: media that no source or extract references;
- **Uncited assertions**: the individuals, events, attributes, relationships and places that carry no citation. A big file can have tens of thousands of them, so the tab opens on a **summary by type** (how many are uncited, out of how many, and the share that is cited). Double-click a type, or *All types*, to list its records, by name; **Back** returns to the summary, and the search field filters the list by id, type or name.

**Export CSV...** writes the rows of the tab in view to a file (what the list shows, with its order and filter; from the summary of the uncited tab, every uncited record). The file is UTF-8 and opens in Excel with the accents right.

## Documentation coverage

**Source > Documentation Coverage...** is the dashboard of how well the file is backed by evidence. It never changes the file; double-click a record to open it and fix the gap, and the figures are read again afterwards. It has three tabs:

- **By record type**: for individuals, families, events, places and media, how many there are, how many have no citation and the share that is documented. Select a type to list its undocumented records (the first 2000 if there are more). An *individual* is documented when it, or one of its events, attributes or relationships, carries a supporting citation (the facts of a person are cited, rarely the person); a *family* is a partnership between two individuals, documented when the relationship itself, or the *marriage* event of the couple, carries a citation (the list says so under the type); a *medium* is documented when a source or an extract refers to it. A citation that *contradicts* what it is attached to documents nothing;
- **Hotspots**: the places where most events have no citation, and the same by kind of event (births, deaths, marriages...), the worst first;
- **Branches**: a branch is an individual with no known parent and all their descendants; the branches are listed from the least documented, with how many individuals each has and how many are documented. A branch with the same members as another (a founder and the partner who married into the family) is listed once.

What the privacy policy hides is neither counted nor listed. The report section *Coverage* gives the same kind of figures for the subject of a report.

The rows can be opened (double-click) to fix the problem on the spot; the lists are made again afterwards. The command is available whenever a file is open: in a file with no source at all, every assertion is uncited.

## Where a fact comes from (provenance)

**Source > Provenance...** shows the chain from an assertion to its evidence, one row for each citation: the *source* (with the repository that holds it), the *locator*, the *stance* (a citation that **contradicts** the assertion is in red), the *extract*, and the *evidence* qualifiers written out (for example *original, primary, direct*). The evidence column is empty when the citation has not been assessed.

- It works on the selected record; if nothing is selected it asks for an individual, a group, an event or a source. It is also in the menu of an individual in the tree and in the ego network.
- For an **individual**, the check box *Include events, attributes and relationships* adds the citations of the events they took part in, of their attributes and of their relationships. Off, only the citations of the individual itself are listed.
- *Explain* opens the evidence trace (see below) of the assertion in the selected row.
- For a **source** it works the other way round: everything that cites it, to see what depends on it. The sources derived from it are in the list too (as records of type source), so a transcription can be followed back to its original and an original forward to its copies. On a source that is itself a copy, the rows are the sources it is *derived from*.
- The line above the table counts the citations, how many support and how many contradict, and in how many the evidence is assessed. A record with no citation says so: nothing says where it comes from.

A row can be opened (double-click) to fix the citation; *Open source* opens its source. The list is read again afterwards. Nothing here changes the file.

## Why do we believe this? (evidence trace)

**Source > Evidence Trace...**, or **Explain This...** in the *Analyze* submenu of the right-click menu of an individual (from the *Standard* level of features), answers the question for a fact: which citations back it, which contradict it, and what the researcher concluded. It is the chain *assertion -> citation -> extract -> source*, read for one record; for an individual it explains each fact about them (the individual itself, their events, attributes and relationships), listed on the left with how its evidence stands.

- The table lists the assertion and the assertions that **compete** with it (several births, burials or deaths of the same person, two values of a single-valued attribute such as sex, relationships of different types between the same two people): one row for each citation, with the source and locator, the *stance* (red if it contradicts), the extract, the evidence qualifiers, the *weight* of the citation and, in the assertion, its confidence and whether a conclusion *prefers* it. An assertion with no citation says so.
- The weight is a rule of thumb: one for a citation that supports, plus one for an original source, one for primary information and one for direct evidence; minus one for a citation that contradicts or is negative evidence.
- Below, the **reasoning path** lists, step by step, what the evidence says, the conclusions that evaluate the competing assertions (a conclusion *settles* the conflict when it resolves at least two of them and prefers one, or says that none prevails) and where it leads: *undocumented*, *contradicted*, *uncontested*, *settled*, *best supported* or *open*. It never decides: it only reads the evidence, the judgement stays in the conclusions.
- A row can be opened (double-click) to fix the record; *Open source* opens its source; the trace is read again afterwards. On a source, the command opens the Source Workspace instead. Nothing here changes the file.

## Conflict dashboard

**Source > Conflict Dashboard...** (from the *Standard* level of features) is the one place where everything that is in conflict or still open is listed, so you can work through it instead of looking for it. It has the same knowledge as the Research Assistant and the *Conflicts* report, seen as a list to work on:

- **Dates** (births, baptisms, deaths and burials of one person whose dates cannot all be right, or that are several and not compared yet), **places** (the events whose places cannot all be right), **relationships** (more than two biological parents, two of the same sex, someone who is their own ancestor, a child and a parent related in ways that exclude each other), **attributes and names** (several values of what can have only one), **identities** (hypotheses that no conclusion has judged) and **open conclusions** (a conclusion that evaluates records but prefers none and states no proof status that decides, so it settles nothing).
- The three filters at the top choose the **status** (all, only the unresolved, only the settled), the **confidence** (a conflict is listed when at least one of its assertions has it, or has none for *Not stated*) and the **record type** (events, relationships, attributes, names, identity hypotheses). They are the same filters of the *Conflicts* report. A conclusion is listed with the type of the records it evaluates.
- Selecting an entry shows below the competing assertions, each with its *confidence*, how many citations *support* it and how many *contradict* it (the stance of the citations), and which one a conclusion *prefers*.

The resolution tools are the buttons: **Open record** opens the selected assertion (or the individual, if no assertion is selected); **Explain** opens its [evidence trace](#why-do-we-believe-this-evidence-trace); **Create conclusion...** starts a new conclusion that already resolves the competing records, with the issue and a description of the conflict (it settles nothing until you choose the preferred assertion or the proof status), and becomes **Open conclusion** when a conclusion already deals with the entry. A double-click does the same as that button. Names have no conclusion (a conclusion cannot point to a name): fix, merge or retype them instead. The dashboard never changes the file by itself, and it is read again after you edit something. What the privacy policy hides is left out.

## Bibliography

**Reports > Bibliography...** produces the list of the sources of the file. See [Reports](reports.md).
