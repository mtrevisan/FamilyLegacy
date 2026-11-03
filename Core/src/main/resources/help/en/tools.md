# Tools

The **Tools** menu holds the commands that look after the quality of the file as a whole.

## Duplicates and merging

**Detect Duplicates...** finds individuals that may be the same person, and **Merge Individuals...** merges two of them. Both are explained, with the scoring and the rules of the merge, in [Finding duplicates and merging people](merging.md).

## Validate File

**Validate File...** runs the full FLEF validation on the document and shows a report. Each problem names the record it concerns, and the record can be opened from the report. The validation runs in the background, so even a large file does not freeze the window. If the grammar file cannot be found, the tool says so and does not validate.

## Statistics

**Statistics...** is available whenever a file is open. It is computed in the background, with a progress window, and the result has three tabs. Records hidden by the [privacy](privacy.md) policy are not counted. A line at the bottom reports the number of selected rows, the sum of their occurrences and its percentage over the relevant total.

**Completeness.** A header counts the individuals, events and sources. The table counts how many individuals are missing something:

| Category | What it counts |
|---|---|
| **Without birth date** | Individuals that have no birth event (it does not check that the event has a date). |
| **Without death date** | Individuals that have no death event, the same way. |
| **Without parents** | Individuals that are not the child of anybody; any child link counts, adoptive and step ones included. |
| **Without source citation** | Individuals whose own record has no citation (citations on their events or relationships do not count). |
| **Without recorded sex** | Individuals with an empty sex. |

Double-click a category to list those records. **Open record** (or a double-click) opens the editor.

**Distribution.** The count of males, females and unknown, and two tables, **Top 20 surnames** and **Top 20 given names**, with the columns *Canonical*, *Variants* and *Total*. All the names of an individual count, not only the first. Names written in a similar way are **grouped**: the most frequent spelling becomes the canonical form, and a spelling that is very similar to it (an exact match, or a very high similarity of letters) is counted as one of its variants. The *Total* of a group is the sum of its variants.

**Source coverage.** *Coverage by record type* says, for every type of record in the file, how many there are, how many have at least one citation, and the percentage. *Most cited sources* lists the ten most cited; a double-click opens the source.

## Consanguinity Report

It lists the couples whose partners are related and the people who are the child of such couples. See [Kinship and consanguinity](kinship.md#consanguinity-report).

## Research Assistant

**Research Assistant...** (`Ctrl+Shift+A`) is described in [The Research Assistant](assistant.md).

## Related tools in other menus

- **Individual > Kinship Calculator...**: [Kinship and consanguinity](kinship.md).
- **Group > Group Timeline...**, **Group > Group Network...** and **Individual > Group Career...**: [Group timeline, group network and group career](group-views.md).
- **File > Backup...** and **Restore from Backup...**: see [Files](files.md#backup-and-restore).
