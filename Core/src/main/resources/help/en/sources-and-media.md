# Sources and media

## Sources

A **source** is where information was found: a parish register, a census, a letter, a book. The **Source** menu has:

- **Manage Sources...** opens the list of sources, where you can search, create, edit and delete them. It is available when the file has sources.
- **New Source...** creates a source.

## Repositories

A **repository** is the place that keeps a source: an archive, a library, a parish office.

- **Manage Repositories...** lists them.
- **New Repository...** creates one.

## Media

A **media** record points to a file: a scan, a photograph, a document.

- **Manage Media...** lists the media with their location, description and the number of records that reference them, and shows a preview of the selected image. You can search, create, edit and delete media. Only local files are previewed, to keep the dialog responsive.
- The location can be a path on your disk, a `file:` address or a web address. How the location is *shown* is a preference: see [Preferences](preferences.md#media-paths).

## Citations

A **citation** ties a statement to its source and says where in the source it is found (the *locator*: volume, page, entry).

- **Source > Add Citation...** attaches a citation to a record: you choose the record, then the source, fill in the locator and decide whether to add an extract. It is a starting point for when you know the source but do not want to open the record first.
- Citations can also be added from the **Sources** tab of any editor, where **Add Citation** takes an existing source or creates one, and where you can add an extract with the text of the source.
- The [Source Citation Assistant](assistant.md#the-source-citation-assistant) turns an archival reference such as *vol. 3, p. 45, n. 12* into a locator.
- How reliable the evidence is (original or derived source, primary or secondary information) is recorded with *evidence qualifiers*, see [Research](research.md#evidence-qualifiers).

## Checking how sources are used

**Source > Citation Analysis...** shows, in three tabs:

- **Unused sources**: sources that no record cites;
- **Unused documents**: media that no source or extract references;
- **Uncited assertions**: how many records of each type carry no citation.

The rows of the first two tabs can be opened (double-click, or *Open record*) to fix the problem on the spot.

## Bibliography

**Reports > Bibliography...** produces the list of the sources of the file. See [Reports](reports.md).
