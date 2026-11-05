# Tools

The **Tools** menu holds the commands that look after the quality of the file as a whole.

## Duplicates and merging

**Detect Duplicates...** finds individuals that may be the same person, and **Merge Individuals...** merges two of them. Both are explained, with the scoring and the rules of the merge, in [Finding duplicates and merging people](merging.md).

## Validate File

**Validate File...** runs the full validation of the document and shows a report: the rules of the FLEF grammar, and the checks on the structure of the records that the grammar cannot express:

- **Circular parentage** (error): individuals who are their own ancestors through the biological child links. Each circle is reported once, naming the individuals in it (the first six), not once for each of them.
- **More than two biological parents** (error): an individual has at most two, whatever their sex; the same parent written twice counts once.
- **A sibling that is also a parent** (error): two individuals who share a biological parent are siblings, so one cannot be the biological parent of the other.
- **Two biological mothers or two biological fathers** (error): the two parents of an individual are both recorded with the same sex. The message says that either the sex of one of them or the parentage is wrong: the file cannot tell which. If the sex of a parent is missing (or *unknown*) nothing is checked.
- **A conclusion that contradicts itself**: the preferred record is not among the ones it resolves (error: the protocol says it must be). A conclusion that *disproves* and resolves fewer than two records, or has a preferred one, is a warning (the protocol says it normally should not).
- **A relationship that cannot link what it links** (error): the type is not valid between the two kinds of record (a spouse that is a group, a membership between two individuals).
- **A circular hierarchy** (error): groups that are part of each other, or places that are part of each other (administrative, geographic, ecclesiastical, judicial or cadastral). Places that succeed each other in a circle are a warning, because the dates may explain it. Each circle is reported once.
- **A relationship written twice** (warning): the same type between the same two records; a couple written from both sides counts as one.
- **A relationship that ends before it begins** (error): every day of its end is before every day of its beginning, whatever the calendar and the precision of the dates.
- **An event nobody took part in** (warning), and a birth, baptism, death, burial or cremation whose participants all have a role and none is the *principal*, the one the event is about (warning). If a participant has no role nothing is said: the role is optional.
- **A media whose file is not there** (warning): the location is looked for on the disk. A web address is not looked for, and a relative path is looked for under the base directory (the one chosen in Appearance > Media paths, or the folder of the open file); with no base directory a relative path is not judged. It can take time on a network drive, which is why the validation runs in the background.
- **Dates that cannot be right** (error), whatever the day the dates may stand for: a death before the birth; an event after the death, or before the birth, of one who took part in it (the burial, the death itself, the probate and the like may follow the death); a child born before a parent (an adoptive, foster, guarded or step child may be older), or after the death of the mother, or more than 300 days after the death of the father; a union that begins before a partner was born or after they died; a membership of a group that begins before the member was born or after they died, or ends before they were born. A date is rarely exact, so each is judged by **all the days it may stand for**: a year is a year, a decade ten years, a century a hundred, an approximate date has its margin (two years if it states none), a bounded one lies between its two days, an open one has an end missing. A problem is reported only if every day of one date is before every day of the other: a death in the 1850s is not before a birth in 1855, and a death in the 18th century is not before a birth in the 18th century. With several births (or deaths) of the same person all the days they cover are used. A date that cannot be read is not judged here. The ages (a mother of twelve, a life of over 120 years) are conventions, and are findings of the [Chronology Assistant](assistant.md), not errors of the file.
- **Dates that cannot be read** (warning): a date that is not valid in its calendar (*31 February*), and a date whose calendar needs the Ephemeris plugin, which is not installed (the Chinese or the Armenian one): the second is **not checked**, and the file is not wrong; the two are in different groups of the report. They are the dates of the events and of the relationships.
- **A relationship with itself** (error): the subject and the target of a relationship, or of a place relationship, are the same record.
- **An individual without a name** (warning).

The report is **grouped by the kind of problem** ("Circular parentage (2)", "Media files not found (300)"), because a file is corrected one kind at a time and a kind with hundreds of rows must not bury the others. The groups are **closed** when the report opens, so that you first see which kinds there are and how many problems each has; click the line of a group to open or close it (what you opened stays open when you press Re-run). The groups with errors come first, then the ones with warnings only, and in each the smaller groups before the larger. What the grammar finds is grouped by the type of the record it is about. **Show** filters the errors only, or the warnings only. Each row has its severity (an error in red, a warning in orange). A file with warnings only is valid. The structural checks always run, even when the grammar finds problems, because they do not depend on it. Each problem names the record it concerns, and the record can be opened from the report. The report **opens at once**, empty, with a progress bar and its percentage, and the problems appear when the validation is done: you see where it is and know that something is happening. The validation runs in the background, so even a large file does not freeze the window; closing the report stops it, and **Re-run** validates again with the same bar. If the grammar file cannot be found, the tool says so and does not validate.

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

## Date tools

**Calendar Converter...**, **Date Calculator...** and **Historical Date Assistant...** are described in [Events and dates](events-and-dates.md#date-tools-in-the-tools-menu).

## Research Assistant

**Research Assistant...** (`Ctrl+Shift+A`) is described in [The Research Assistant](assistant.md).

## Related tools in other menus

- **Individual > Kinship Calculator...**: [Kinship and consanguinity](kinship.md).
- **Group > Group Timeline...**, **Group > Group Network...** and **Individual > Group Career...**: [Group timeline, group network and group career](group-views.md).
- **Place > Jurisdiction Timeline...**: how a place changed jurisdiction in time, in the same page.
- **File > Backup...** and **Restore from Backup...**: see [Files](files.md#backup-and-restore).
