# Sources and media

## Sources

A **source** is where information was found: a parish register, a census, a letter, a book. The **Source** menu has:

- **Manage Sources...** opens the list of sources, where you can search, create, edit and delete them (see [Manage dialogs](searching.md#manage-dialogs)). Each line shows the author, the type and how many citations the source has: a source with no citation is one to look at. It is available when the file has sources.
- **New Source...** creates a source.

## Repositories

A **repository** is the place that keeps a source: an archive, a library, a parish office.

- **Manage Repositories...** lists them, in the same way (see [Manage dialogs](searching.md#manage-dialogs)), with the place and the number of sources each one keeps.
- **New Repository...** creates one.

## Media

A **media** record points to a file: a scan, a photograph, a document.

- **Manage Media...** lists the media with their location, description and the number of records that reference them, and shows a preview of the selected image. You can search, create, edit and delete media. Only local files are previewed, to keep the dialog responsive.
- A media can have a **date**: when the photograph, recording, scan or document was created or captured. It matters when that date has historical relevance, and it is edited like any other date.
- The location can be a path on your disk, a `file:` address or a web address. How the location is *shown* is a preference: see [Preferences](preferences.md#media-paths).

## Citations

A **citation** ties a statement to its source and says where in the source it is found (the *locator*: volume, page, entry).

- **Source > Add Citation...** adds a citation to a record: you choose the record (an individual, group, event, place or relationship, shown by name), then the source, and fill in the locator and the stance (*supports* or *contradicts*). The citation is written in the record when you press *Continue*, with that locator and stance; you are then offered to open the record, to complete it with an extract or the evidence qualifiers.
- Citations can also be added from the **Sources** tab of any editor, where **Add Citation** takes an existing source or creates one, and where you can add an extract with the text of the source.
- The [Source Citation Assistant](assistant.md#the-source-citation-assistant) turns an archival reference such as *vol. 3, p. 45, n. 12* into a locator.
- How reliable the evidence is (original or derived source, primary or secondary information) is recorded with *evidence qualifiers*, see [Research](research.md#evidence-qualifiers).

## Checking how sources are used

**Source > Citation Analysis...** shows, in three tabs:

- **Unused sources**: sources that no record cites;
- **Unused documents**: media that no source or extract references;
- **Uncited assertions**: the individuals, events, attributes, relationships and places that carry no citation. A big file can have tens of thousands of them, so the tab opens on a **summary by type** (how many are uncited, out of how many, and the share that is cited). Double-click a type, or *All types*, to list its records, by name; **Back** returns to the summary, and the search field filters the list by id, type or name.

**Export CSV...** writes the rows of the tab in view to a file (what the list shows, with its order and filter; from the summary of the uncited tab, every uncited record). The file is UTF-8 and opens in Excel with the accents right.

The rows can be opened (double-click, or *Open record*) to fix the problem on the spot; the lists are made again afterwards. The command is available whenever a file is open: in a file with no source at all, every assertion is uncited.

## Where a fact comes from (provenance)

**Source > Provenance...** shows the chain from an assertion to its evidence, one row for each citation: the *source* (with the repository that holds it), the *locator*, the *stance* (a citation that **contradicts** the assertion is in red), the *extract*, and the *evidence* qualifiers written out (for example *original, primary, direct*). The evidence column is empty when the citation has not been assessed.

- It works on the selected record; if nothing is selected it asks for an individual, a group, an event or a source. It is also in the menu of an individual in the tree and in the ego network.
- For an **individual**, the check box *Include events, attributes and relationships* adds the citations of the events they took part in, of their attributes and of their relationships. Off, only the citations of the individual itself are listed.
- For a **source** it works the other way round: everything that cites it, to see what depends on it.
- The line above the table counts the citations, how many support and how many contradict, and in how many the evidence is assessed. A record with no citation says so: nothing says where it comes from.

A row can be opened (double-click, or *Open record*) to fix the citation; *Open source* opens its source. The list is read again afterwards. Nothing here changes the file.

## Bibliography

**Reports > Bibliography...** produces the list of the sources of the file. See [Reports](reports.md).
