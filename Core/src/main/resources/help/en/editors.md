# The record editors: what they have in common

Every record of the file (an individual, an event, a source...) has its own editor. This page describes what all the editors share; the pages that follow describe the fields of each one:

- [People and groups](editors-people.md): individuals, groups, their attributes and their names.
- [Events, places and context](editors-events-places.md): events, participations, places, historic events, cultural norms and context impacts.
- [Sources and media](editors-sources.md): sources, repositories, media, citations, notes and contacts.
- [Research records](editors-research.md): conclusions, identity hypotheses, questions, activities and tasks.

Relationships and dates have their own pages: [Relationships](relationships.md) and [Entering dates](dates.md).

## The window

- The title says *New* or *Edit* and the type of the record. **OK** is at the bottom and confirms the record; there is no Cancel button: `Esc` (or the close button of the window) closes the editor and discards the changes. Once confirmed, the change can be reverted with Undo (`Ctrl+Z`).
- **An editor opened from another editor** (a name inside a person, a source from a citation, an attribute or a participation from the tabs of a person...) says, in grey beside **OK**, that its change is **kept only if you confirm the parent with OK**: closing the editor that opened it with `Esc` discards everything done below it, including the records it created or changed (a new attribute, a new source...). This is the same for every editor, whatever the kind of record. Once the parent is confirmed, all can be reverted with Undo (`Ctrl+Z`).
- Every field label ends with a colon. A label with an **asterisk** (`Type*:`) marks a required field. A list title never has an asterisk: a list (for example the *Personal Names* of a person) may be empty.
- On OK, the editor checks the fields that it really requires. If one is missing, a dialog titled **Validation Error** says which, and the focus goes to that field (on its tab). The checks are listed in the page of each editor. Some fields have an asterisk but are not checked on OK: the file may then contain a record without them, and [Tools > Validate File](tools.md#validate-file) may report it.
- A combo box that is **editable** accepts a value of your own as well as the listed ones. A combo that is not editable accepts only the listed values. A combo with a blank first item allows "no value". A value without a translation is shown with a capital letter and spaces instead of underscores. When a combo has many values (more than twelve) you can **search** it: with the combo focused, just type, and only the values containing what you typed (word by word, ignoring case) are listed; Backspace removes the last letter, Enter picks the highlighted value, and Esc closes the list.
- A combo box with many values (more than 12) is **searched while you type**: the list shows only the values that match what you typed (word by word, any case). In an editable combo the text stays as you typed it, and you can keep it if it is none of the listed values.
- Hovering a value or a field often shows a tooltip that explains it. Hovering the first tab shows a short *when to use this record* hint; hovering any other tab says what it is for (the tabs of the tool dialogs, such as Statistics, the Source Workspace and the Historical Date Assistant, do the same).

## The tabs

Which tabs an editor has depends on the record; their order is always the same:

1. **Properties**: the fields of the record itself.
2. **Cause** and **Location**: only for events and places.
3. **Attributes**: the attributes of an individual or a group.
4. **Relationships**: the relationships where the record is the subject (*Relationships*) and where it is the target (*Inverse Relationships*; for a group *Group Memberships*, for a place *Part of* and *Contains*).
5. **Participations**: the event participations of the record.
6. **Context**: the context impacts that concern the record.
7. **Research**: conclusions, identity hypotheses and research questions about the record.
8. **Findings** and **References**: only for research activities.
9. **Sources**: see below.
10. **Stories** (only for an individual, a group, an event, a place and a source, from the standard level) and **Notes**, **Tags**, **Privacy** and **Audit**: see below.

The lists on these tabs are the linked records. Each list has a context menu (right-click) with **Create New...**, **Add Existing...**, **Edit...** and **Remove**. Every list can be arranged: select an item and use **Move Up** and **Move Down** of the menu, `Ctrl` + `Up` and `Ctrl` + `Down`, or drag the item to another place. The order is the one of the file, saved with **OK**: a list of notes or stories is kept in the order you gave it, and the reports write them in it (the stories, the notes, the media); a list of records (the attributes, the relationships, the participations...) puts those records in that order in the file, but a view or a report that sorts them (the events by date, for example) keeps its own order. In the *Sources with Citations* lists, double-click edits the citation and `Shift` + double-click edits the source.

If an editor has only the Properties tab, no tab bar is shown.

## Sources with Citations

The **Sources** tab lists the citations that support the record. The menu offers **Cite a New Source...** (the short way to cite a source that is not in the file yet: see [the quick citation](editors-sources.md#cite-a-new-source)), **Create New...**, **Add Existing...**, **Edit Record...**, **Edit Citation...** and **Remove**. Under the list, an image carousel shows the images of the selected source, if it has any. See [Sources and media](sources-and-media.md) and [the citation editor](editors-sources.md#source-citation).

## Media of a record

The editors of an individual, group, place, event and repository have a **Media** list (in the *Sources* tab, or under the sources) with the photographs, scans, maps and other media that illustrate the record. Right-click offers *Add Existing...* (choosing or creating a media record, then the page or the crop to use), *Edit Crop...*, *Edit Caption...* and *Remove*. The same media can be listed by several records, or twice by the same one, each time with its own **caption**, crop and page: the caption says what the media shows *for that record* ("Family house" on a place, "Childhood home of Giovanni Rossi" on an individual). Only the **preferred image** of an individual or group is the picture shown in the boxes of the tree; the media list does not change it. The *Media* section of a report also lists the media of the record. A media list is not evidence: that is what the citations are for. To see where a media is used, open it and press **Where Used...**.

## Confidence

Many records have a **Confidence** combo: blank, **low**, **medium** or **high**. It says how sure you are that the assertion is true, judged from the citations that support it; it is your current estimate and can change as evidence is added.

- *low*: limited evidence, unresolved conflicts, or speculation.
- *medium*: the evidence generally supports it, but gaps or uncertainties remain.
- *high*: the evidence strongly supports it, and no significant conflict is known.

## Notes

A **Notes** tab lists the notes of the record, each edited in its own dialog: see [the note editor](editors-sources.md#note). The editor of an individual, a group, an event, a place or a source has the notes of kind *story* in a list of their own, above the other notes in the Notes tab, so that narrative prose is not mixed with the technical notes of the research (below the standard level the Notes tab has the one list of all of them).

## Tags

Individuals, groups, places, sources, repositories, research questions, research activities and research tasks have a **Tags** tab: your own words to classify the record, for what is temporary, specific to a project or cuts across the kinds of record and does not deserve a record of its own (*Brick Wall*, *DNA Candidate*, *To Verify*, *Needs Translation*, *Military Research*, *Immigration*, *Open Conflict*, *FAN Cluster*...).

- The tags are small rounded boxes in one area, followed by a free space to type in: type a tag and press `Enter` (several at once, separated by commas); `Down` lists the tags that other records of the file already have. Click the **×** of a box, or select it (click, or the arrows; `Backspace` in the empty field goes back to the last one) and press `Delete`, to take it away. **OK** gives the tags to the record; `Esc` leaves them as they were.
- A tag is **free text**: there is no list of allowed tags. Spaces at the ends are dropped, and two tags that differ only by capitals are the same tag (the spelling you wrote first is kept).
- **The tags are not in the file.** They are not part of the FLEF protocol: the application keeps them next to the file, in a folder named after it (`family.flef.meta`, with a `tags.properties` inside). **File > Save As** and **Backup** copy the folder too; when you copy, move or archive the file, take the `.meta` folder with it. A file sent alone, without that folder, has no tags; a file renamed outside the application loses them unless you rename the folder too. Tags kept by older versions in your user folder are brought next to the file the first time it is opened. The tags are not covered by Undo (see [Undo](undo.md)).
- The tags are found by the search (see [Find record](searching.md)), listed by the **Tagged Records** report (see [Reports](reports.md)), shown by the [Research Assistant](assistant.md) for the record you are looking at, and joined when two records are merged (see [Merging](merging.md)).

## Privacy

The **Privacy** tab sets how the record is treated when the privacy policy of the views or of an export is applied:

- **Level**: *public*, *restricted* or *confidential*.
- **Reason**: *living person*, *privacy law*, *copyright*, *repository license* or *sensitive information*; it is editable, so you can write your own.
- **Expires**: the date from which the restriction no longer applies. Write it as `2030-05-31`, `2030-05` or `2030`; any other text is not understood and the restriction then never expires.

What the levels and the policies do is explained in [Privacy](privacy.md); the view setting is in [Navigating the tree](navigating.md#privacy).

## Audit

The **Audit** tab has the **Creation Comment** (with the creation date) and the list of **Updates**, each with a date and a comment. When you confirm a change to a record that has an audit, the application adds an update by itself, with the date and a summary of what changed.

## Validity range

Where a fact holds for a period (an attribute, a relationship, a name), a box named **Validity Range** has **Valid From** and **Valid To**. They are [date fields](dates.md).
