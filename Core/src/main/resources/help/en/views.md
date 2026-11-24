# The tree, the graph and the ego network

These are the three views of the family around the selected person. Switch between them with the toolbar buttons (**Family Tree**, **Sugiyama Graph**, **Ego Net**), the **View > Projection** menu, or `Ctrl+1`, `Ctrl+2`, `Ctrl+3`. The fourth view is the [Chronomap](chronomap.md). The shortcuts and the moves common to the views are in [Navigating the tree](navigating.md).

## What the boxes do

The same mouse actions work in the three views:

| Action | Result |
|---|---|
| Click the **name** of a person in a box, right on its letters, even when the name is just `?` (the pointer becomes a hand) | The view is **re-rooted** on that person. |
| Click elsewhere on a box, including the blank space beside or below the name | Selects it; the sidebar shows that person. |
| **Double-click** a box | Opens the editor. |
| **Right-click** a box | Selects it and opens its context menu. |
| Drag any empty part of the view | Moves the view. There is no wheel zoom in these views: use the scroll bars or drag. |
| Hover | The border of the box lightens, and a tooltip shows the name and the dates. |

The selected box has an orange-red border. Empty placeholder boxes (an unknown parent) ignore the mouse, except where a context menu offers to fill them (see [Relationships](relationships.md#how-to-record-the-usual-cases)).

**Keyboard.** The **arrow keys** move the selection to the nearest box in that direction, and **Enter** re-roots the view on the selected box. They work when the focus is not in a text field. `Ctrl+Left` and `Ctrl+Right` go back and forward in the history, and `F2` edits the selection.

**Pedigree collapse.** A small red disc with a number and a cross (`2×`) on a box says that the same person appears that many times in the tree you see, because of a shared ancestor. The tooltip explains it. In the Family Tree, moving the mouse over one of those boxes outlines in red every other box of the same person, so you can see where the repetitions are. `Ctrl+P` opens a report of the pedigree collapse.

## Tree

The **Family Tree** is a classic pedigree: one box per individual, couples side by side, an unknown ancestor or partner is not drawn (there is no empty box): add the missing person from the context menu of the person that is there (**Add > Parent...**, **Add > Partner...**, or their **Connect** versions).

- A solid thin line joins a birth connection; a **dashed line** joins an adoptive or other non-birth connection.
- The small square between the two partners is the **union**. Hover it to see every marriage the two share (civil, religious, ...), each with its date and place, earliest first. Its border is **solid** when at least one marriage is recorded and **dashed** when none is.
- `Ctrl+L` switches the layout between vertical and horizontal.
- `Ctrl+K` opens the kinship dialog between the root and a second person (see [Kinship and consanguinity](kinship.md)).
- The number of ancestor levels is fixed; no control changes it.

### The Lifespans bar

At the bottom there is a collapsed bar called **Lifespans**. Click it, or press `Ctrl+T`, to open it. It has one row for each person visible in the tree:

- the name, on the left;
- a grey bar from the earliest to the latest **dated** event of that person, with a small coloured marker for each dated event (the colour depends on the type; birth and death are not told apart);
- thin coloured bands above the bar for the attributes that have a validity interval (residence, occupation, title...);
- a background banded by century.

**Hover** a row to see the type, date and place of an event; the rows whose life overlaps the hovered one get a blue strip, and the others are dimmed. **Click an attribute band** to edit the attribute; **double-click an event marker** to edit the event. `Ctrl` + the wheel zooms at the cursor, the plain wheel scrolls the rows, a drag moves along the axis, and a click on the axis resets the zoom.

## Sugiyama graph

The **Sugiyama Graph** shows the same pedigree as a layered graph. A person reached through several lines appears **once**, with several connections: endogamy and shared ancestors show up as lines that converge instead of repeated boxes (the pedigree-collapse disc marks such people). It has the same mouse actions, keys (`Ctrl+L`, `Ctrl+T`, `Ctrl+K`, `Ctrl+P`), context menu and **Lifespans** bar as the tree. There is no legend.

## Context menu of the tree and the graph

Right-click a box:

- **Edit Individual...**
- **Add** (submenu): **Partner...**, **Parent...**, **Child...**
- **Connect** (submenu): **Partner...**, **Parent...**, **Child...**
- **Paste as** (submenu): **Partner**, **Parent**, **Child**
- **Relocate**, **Paste**, **Unlink Relationships...**
- **Analyze** (submenu): **Memberships Over Time...**, **Provenance...** and **Explain This...** (from the *Standard* level of features), **Consanguinity Loops...**
- **Delete**

The entries are enabled or disabled according to the box (a submenu is grey when nothing in it applies). *Add > Partner...* and *Add > Parent...* open the "new individual" dialog, then ask the type of the relationship and create it; *Connect > Partner...* and *Connect > Parent...* do the same with an existing person. The partner entries are always available (a person may have several partners); the parent entries only while the person has fewer than two parents. What they do is explained in [Relationships](relationships.md). *Paste as > Partner*, *Paste as > Parent* and *Paste as > Child* use the person copied with **Relocate**: they detach it from everyone and link it to the selected person (as in [Relationships](relationships.md#moving-and-removing-links)). When the selected person already has a parent of a known sex, a new parent must be of the opposite sex: *Add > Parent...* creates the person with that sex and the editor does not save another one, and *Connect > Parent...* and *Paste as > Parent* refuse a person of the same sex.

## Ego network

The **Ego Network** is a hub with spokes around one individual or group, the *ego*: the parents, partners, children, associates and the groups the ego belongs to, each as a box joined by a stepped line. Re-root by clicking a name, or by selecting a box with the arrow keys and pressing Enter.

- `Ctrl+L` switches the layout. `Ctrl+T` shows or hides the bottom bar **Events**, a timeline of the events of the people shown, with the historic events and cultural norms of the places where they lived behind the rows (hover an empty spot of a row to read them).
- A small amber disc with a **C**, in the middle of the line to a partner, marks a possible **consanguineous union** with the ego; the tooltip of the partner explains it.
- All the lines are drawn dotted in this version.
- **Jump to** also finds groups in this view.

Context menu of a **person**: *Edit Individual...*, the submenus *Add*, *Connect* and *Paste as* (Partner, Parent, Child; only for the ego), *Paste*, *Unlink Relationships...*, the submenu *Analyze* (*Memberships Over Time...*, *Provenance...*, *Explain This...*, *Consanguinity Loops...*) and *Delete*. Of a **group**: *Edit Group...*, *Add Members...*, *Paste*, *Unlink Relationships...*, *Group Timeline...*, *Group Network...*, *Delete*.

## The sidebar

The right-hand pane, called the **Dossier**, is shown with the toolbar button **Sidebar** or **View > Show Sidebar**. Until you select somebody it says *Select an individual to view its dossier*. Then its title is the name of the selected individual or group, with the *Vital* line (the years of life) under it.

For an individual the sections are, in this order (a section with nothing in it is left out, and the header shows the number of entries): **Identity** (the parts of the name, photo, sex, vital, privacy), **Events**, **Attributes**, **Relationships**, **Friends, Associates, Neighbors**, **Sources**, **Media** (the media that the individual cites and its preferred image: the row says how many regions the individual is tagged in, "2 regions", and whether the media is its "portrait"; a click opens the editor on the *Sources* tab with the citation selected, or on the *Properties* tab for a preferred image that is not cited), **Context**, **Identity hypothesis** (including *Possible duplicate*), **Research**, **Stories**, **Mentioned in stories** and **Notes** (the stories are the notes of kind *story*, kept apart from the other notes; *Mentioned in stories* lists the stories written in other records that link to this one with `[text](flef:<ID>)`: a click opens the editor of the record the story is in, on the story). For a group: **Identity**, **Members**, **Subgroups**, **Attributes**, **Events**, **Sources**, **Media** (the media the group cites and its preferred image), **Context**, **Research**, **Stories**, **Mentioned in stories** and **Notes**.

- Only *Identity* and *Events* start open; the others are collapsed. The open or closed state of each kind of section is remembered for the session.
- A click on a section header opens or closes it.
- **Friends, Associates, Neighbors** (the FAN method of E. S. Mills, to get around a brick wall) is a table of the people who appear together with the individual in the documents, the strongest link first; it is left out when nobody does, and it starts closed. Godparents and witnesses who recur are often relatives or fellow villagers, and the parish registers note them almost always. The columns are the person, the *Events* they share, the *Roles* (the individual's, then the other person's, with how many times: *principal / witness ×2*), the *Period* (the years of the shared events) and the *Place*. The header (*Events*, *Period*...) sorts the table; a click on the name of a person opens the record of that person, and the tooltip tells the strength of the link, how many times the person has a role other than principal in the whole file (somebody who is witness in dozens of events is probably the sacristan, not a friend) and the shared events.
  - Everybody who takes part, with any role, in an event of the individual is listed, and the links between two participants count too: that is how two godparents of the same child are seen as a possible couple.
  - The strength is two points for each shared event, one for each different pair of roles beyond the first and one if the shared events are not all in the same year (a tie that lasts). Spouses, parents, children and siblings (as the relationships say) are shown in grey after the others, whatever their strength, because they are not what the method looks for.
  - Under the table, *Where to search next* lists hints: a person that recurs with no known kinship (a candidate relative or fellow villager) and two people with the same role in the same event, not known to be related, who might be a couple. They are only text, with the reasoning in the tooltip: nothing is created, linked or changed. The same hints, and who to search together with the individual, are in the [Research Assistant](assistant.md#what-it-looks-for).
  - The table is only as rich as the roles recorded: a file whose events have only principal participants (most imported GEDCOM files) shows nothing but relatives, or no table at all.
- In **Sources**, **Stories** and **Notes**, a row that belongs to an event, an attribute or a relationship says which one on its second line (for example *of birth, 19 Nov 1926* or *of occupation: baker*), so that you can tell apart several of the same kind. A click on such a row opens the note (or the source citation) inside the editor of that event, attribute or relationship; **Shift + click** opens the editor of the event, attribute or relationship itself, on its first tab. The tooltip of such a row reminds you of the two ways.
- A value in **blue** belongs to a record: **click the row** to open the editor of that record, on the tab the row belongs to and with the focus on the right item (for example, the *Sex* row opens the *Attributes* tab with the sex attribute selected); the dossier refreshes after you save. The rows are **not** links to other people: to move to somebody else, use the tree, *Jump to* or *Find record*.
- A date says how sure it is: an exact date is shown as it is, an approximate one as *about 1850*, one derived from another (for instance a birth from the age at death) as *calculated 1850*, and a range as *before 1850*, *after 1850*, *between 1850 and 1860* or *from 1850 to 1860*.
- A row can show a grey subtitle, an evidence badge on the right (such as *orig/prim/dir*, for original, primary, direct), a padlock for a restricted or confidential record, and, for conclusions, a coloured square for the proof status: **green** proven, **blue** supported, **yellow** conflicting evidence, **red** disproven.
