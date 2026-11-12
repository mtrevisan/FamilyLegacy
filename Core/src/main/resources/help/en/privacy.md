# Privacy

Privacy answers one question: *which records must not be shown to somebody else?* It is meant for living people, for sensitive data and for material that is under copyright or under the licence of an archive. It works in two places: what the **views** show, and what a **report** or an **export** writes.

**Privacy is not security.** The file on disk is always complete and in plain text, and the editors always show everything. Only the views, the reports and the FLEF export filter.

## The privacy of a record

Every record can have a **Privacy** tab (see [The record editors](editors.md#privacy)) with:

- **Level**: *public*, *restricted* or *confidential*. Any other value counts as public.
- **Reason**: *living person*, *privacy law*, *copyright*, *repository license* or *sensitive information*. It is editable, so you can write your own. The reason is informative only: it never changes what is hidden.
- **Expires**: the date from which the restriction no longer applies. Only the forms `2030-05-31`, `2030-05` and `2030` are understood; a partial date counts as its **last** day. The restriction holds up to and including that day. Any other text (such as `31/05/2030` or `2030-5`) is not understood, and then the restriction **never expires**.

## The privacy that a record really has

The *effective* level of a record is the most restrictive of these:

1. **Its own block**, if it has one and it has not expired. An explicit *public* block counts: it overrides the living-person rule for that individual.
2. **A default**: a possibly living individual is *restricted*, and an individual attribute of type *national ID* (which includes social security numbers imported from GEDCOM) is *confidential*.
3. **What it refers to**: a record inherits the level of the individuals and groups that appear in it. An event takes the level of its participants, so an event of a hidden person is hidden too; a participation, a relationship, an attribute or a media record takes the level of the people it concerns.

Individuals and groups do **not** inherit from each other: a group is not hidden because one of its members is, and a child is not hidden because a parent is. A source, a place or a repository has a level only if it has its own block, or if it refers to people.

### Living people

An individual is **not** considered living only if one of these is true:

- they are the *principal* of a death event (even with no date);
- they are the *principal* of a birth event dated more than **110 years** ago (the latest day the date allows is used).

Everyone else is **possibly living** and therefore restricted: a person with no events, with a birth date that cannot be read, or born less than 110 years ago. Other events (baptism, burial, marriage, census) are not looked at. No other age rule exists. To open up an individual who is certainly dead, add a death event, or give them an explicit *public* privacy block.

## The privacy policy of the views

**View > Privacy** chooses what the views show. The choice is saved and applied at once.

| Policy | Hides |
|---|---|
| **Show All** (the default) | Nothing. |
| **Hide Confidential** | The records with an effective level of *confidential*. Restricted ones, including possibly living individuals, stay visible. |
| **Hide Restricted and Confidential** | Both, so the possibly living individuals are hidden. |

The policy applies to the tree and the other views, the sidebar, the searches and *Jump to*, the bookmarks, the statistics, the timelines, the chronomap, the kinship and consanguinity tools, the assistants, the boards and the lists of the management tools. It does **not** apply to the editors, which always show everything: to manage a hidden record, switch to *Show All*.

A hidden individual is not removed from the picture: a box is shown, masked (the sex is unknown, the name is a question mark, there is no date and no image). Small padlocks mark the restricted (amber) and confidential (red) records, filled when the level was declared and empty when it was inherited or presumed.

**Relationships of hidden people.** A relationship with a hidden individual is ignored by the views. A hidden parent is not linked to the children, and the children stay visible if they are not hidden themselves, but they show no link to that parent. A hidden relative does not count when the application decides whether a person has parents, children or partners.

## Reports

A report has its **own** privacy choice in the report dialog, independent of **View > Privacy**. It starts at *Hide Confidential* (not at the view's policy), and offers the same three policies as the **View > Privacy** menu. Hidden records, citations and notes are left out, and so are the citations to hidden sources. The option **Show privacy details** prints the level, the reason and the expiry of the records that remain. See [Reports](reports.md).

## Exporting to FLEF

The export to a FLEF file asks first which privacy policy to apply (the default is the current one of the views), and then where to save. The file written is a filtered copy: the open document does not change.

- A record hidden by the policy is left out, and so is every item that refers to it: relationships, participations and attributes of a hidden person disappear.
- Nested items that carry their own hidden level, such as a citation, are removed.
- When something is left out, a dialog says how many records and nested items were omitted.
- In the text of notes, a link written `[text](flef:confidential)` is removed whole (the text too) when any hiding policy is chosen; a link `[text](flef:ID)` to an omitted record becomes `[omitted]`.
- The contacts of the submitter are removed unless they are explicitly public.
- With *Show All* the model is written unchanged.

The export is in the menu **File > Export > FLEF File…**.

## The confidential marker in notes

In the text of a note, `[text](flef:confidential)` marks text to hide when confidential data is suppressed. Write it exactly like this, with the `flef:` prefix: no other form is recognised. The only code that acts on the marker is the FLEF export. It has no effect on screen or in the reports. Do not rely on it to keep anything private.

## What is not filtered

- The editors, always.
- **File > Save as...**, **Backup...** and the `.flef` file itself: they contain everything.
- Import does not touch privacy; it only converts the restriction flags of the source (see [Importing from GEDCOM and Gramps](import.md)).

## Practical advice

- To share a file with relatives, produce a **report** with *Hide Restricted and Confidential*, or a filtered FLEF export, rather than sending the `.flef`.
- Give a death event to the people who are certainly dead, so that they are not treated as possibly living.
- Use *Show All* only when you work on the data, and the stricter policies when you show the screen to someone.
