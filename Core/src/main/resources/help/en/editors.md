# The record editors: what they have in common

Every record of the file (an individual, an event, a source...) has its own editor. This page describes what all the editors share; the pages that follow describe the fields of each one:

- [People and groups](editors-people.md): individuals, groups, their attributes and their names.
- [Events, places and context](editors-events-places.md): events, participations, places, historic events, cultural norms and context impacts.
- [Sources and media](editors-sources.md): sources, repositories, media, citations, notes and contacts.
- [Research records](editors-research.md): conclusions, identity hypotheses, questions, activities and tasks.

Relationships and dates have their own pages: [Relationships](relationships.md) and [Entering dates](dates.md).

## The window

- The title says *New* or *Edit* and the type of the record. **OK** is at the bottom and confirms the record; there is no Cancel button: `Esc` (or the close button of the window) closes the editor and discards the changes. Once confirmed, the change can be reverted with Undo (`Ctrl+Z`).
- Every field label ends with a colon. A label with an **asterisk** (`Type*:`) marks a required field, and a list title with an asterisk (`Personal Names*`) marks a list that needs at least one entry.
- On OK, the editor checks the fields that it really requires. If one is missing, a dialog titled **Validation Error** says which, and the focus goes to that field (on its tab). The checks are listed in the page of each editor. Some fields have an asterisk but are not checked on OK: the file may then contain a record without them, and [Tools > Validate File](tools.md#validate-file) may report it.
- A combo box that is **editable** accepts a value of your own as well as the listed ones. A combo that is not editable accepts only the listed values. A combo with a blank first item allows "no value". A value without a translation is shown with a capital letter and spaces instead of underscores. When a combo has many values (more than twelve) you can **search** it: with the combo focused, just type, and only the values containing what you typed (word by word, ignoring case) are listed; Backspace removes the last letter, Enter picks the highlighted value, and Esc closes the list.
- Hovering a value or a field often shows a tooltip that explains it. Hovering the first tab shows a short *when to use this record* hint.

## The tabs

Which tabs an editor has depends on the record; their order is always the same:

1. **Properties**: the fields of the record itself.
2. **Cause** and **Location**: only for events and places.
3. **Attributes**: the attributes of an individual or a group.
4. **Relationships**: the relationships where the record is the subject (*Relationships*) and where it is the target (*Inverse Relationships*; for a group *Group Memberships*, for a place *Members*).
5. **Participations**: the event participations of the record.
6. **Context**: the context impacts that concern the record.
7. **Research**: conclusions, identity hypotheses and research questions about the record.
8. **Findings** and **References**: only for research activities.
9. **Sources**: see below.
10. **Notes**, **Privacy** and **Audit**: see below.

The lists on these tabs are the linked records. Each list has a context menu (right-click) with **Create New...**, **Add Existing...**, **Edit...** and **Remove**. In the *Sources with Citations* lists, double-click edits the citation and `Shift` + double-click edits the source.

If an editor has only the Properties tab, no tab bar is shown.

## Sources with Citations

The **Sources** tab lists the citations that support the record. The menu offers **Create New...**, **Add Existing...**, **Edit Record...**, **Edit Citation...** and **Remove**. Under the list, an image carousel shows the images of the selected source, if it has any. See [Sources and media](sources-and-media.md) and [the citation editor](editors-sources.md#source-citation).

## Confidence

Many records have a **Confidence** combo: blank, **low**, **medium** or **high**. It says how sure you are that the assertion is true, judged from the citations that support it; it is your current estimate and can change as evidence is added.

- *low*: limited evidence, unresolved conflicts, or speculation.
- *medium*: the evidence generally supports it, but gaps or uncertainties remain.
- *high*: the evidence strongly supports it, and no significant conflict is known.

## Notes

A **Notes** tab lists the notes of the record, each edited in its own dialog: see [the note editor](editors-sources.md#note).

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
