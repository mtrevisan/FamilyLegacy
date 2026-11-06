# The tree, the graph and the ego network

These are the three views of the family around the selected person. Switch between them with the toolbar buttons (**Tree**, **Sugiyama**, **Ego Net**), the **View > Projection** menu, or `Ctrl+1`, `Ctrl+2`, `Ctrl+3`. The fourth view is the [Chronomap](chronomap.md). The shortcuts and the moves common to the views are in [Navigating the tree](navigating.md).

## What the boxes do

The same mouse actions work in the three views:

| Action | Result |
|---|---|
| Click the **name** of a person in a box (the pointer becomes a hand) | The view is **re-rooted** on that person. |
| Click elsewhere on a box | Selects it; the sidebar shows that person. |
| **Double-click** a box | Opens the editor. |
| **Right-click** a box | Selects it and opens its context menu. |
| Drag any empty part of the view | Moves the view. There is no wheel zoom in these views: use the scroll bars or drag. |
| Hover | The border of the box lightens, and a tooltip shows the name and the dates. |

The selected box has an orange-red border. Empty placeholder boxes (an unknown parent) ignore the mouse, except where a context menu offers to fill them (see [Relationships](relationships.md#how-to-record-the-usual-cases)).

**Keyboard.** The **arrow keys** move the selection to the nearest box in that direction, and **Enter** re-roots the view on the selected box. They work when the focus is not in a text field. `Ctrl+Left` and `Ctrl+Right` go back and forward in the history, and `F2` edits the selection.

**Pedigree collapse.** A small red disc with a number and a cross (`2×`) on a box says that the same person appears that many times in the tree you see, because of a shared ancestor. The tooltip explains it. `Ctrl+P` opens a report of the pedigree collapse.

## Tree

The **Biological Tree** is a classic pedigree: one box per individual, couples side by side, with empty boxes where an ancestor is unknown.

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

- **Edit Individual...**, **Add Individual...**, **Connect Individual...**
- **Add Child...**, **Connect Child...**
- **Relocate**, **Paste**, **Delete**
- **Unlink Relationships...**
- **Group Career...**

The entries are enabled or disabled according to the box: *Add Individual*, *Connect Individual* and *Paste* work on an empty box, and *Add Child* and *Connect Child* on a box that has a person. What they do is explained in [Relationships](relationships.md).

## Ego network

The **Ego Network** is a hub with spokes around one individual or group, the *ego*: the parents, partners, children, associates and the groups the ego belongs to, each as a box joined by a stepped line. Re-root by clicking a name, or by selecting a box with the arrow keys and pressing Enter.

- `Ctrl+L` switches the layout. `Ctrl+T` shows or hides the bottom bar **Events**, a timeline of the events of the people shown.
- A small amber disc with a **C**, in the middle of the line to a partner, marks a possible **consanguineous union** with the ego; the tooltip of the partner explains it.
- All the lines are drawn dotted in this version.
- **Jump to** also finds groups in this view.

Context menu of a **person**: *Edit Individual...*, *Add Child...* and *Connect Child...* (only for the ego), *Delete*, *Paste*, *Unlink Relationships...*, *Group Career...*. Of a **group**: *Edit Group...*, *Add Members...*, *Delete*, *Paste*, *Unlink Relationships...*, *Group Timeline...*, *Group Network...*.

## The sidebar

The right-hand pane, called the **Dossier**, is shown with the toolbar button **Sidebar** or **View > Show Sidebar**. Until you select somebody it says *Select an individual to view its dossier*. Then its title is the name of the selected individual or group, with the *Vital* line (the years of life) under it.

For an individual the sections are, in this order (a section with nothing in it is left out, and the header shows the number of entries): **Identity** (the parts of the name, photo, sex, vital, privacy), **Events**, **Attributes**, **Relationships**, **Sources**, **Context**, **Identity hypothesis** (including *Possible duplicate*), **Research** and **Notes**. For a group: **Identity**, **Members**, **Subgroups**, **Attributes**, **Events**, **Sources**, **Context**, **Research** and **Notes**.

- Only *Identity* and *Events* start open; the others are collapsed. The open or closed state of each kind of section is remembered for the session.
- A click on a section header opens or closes it.
- A value in **blue** belongs to a record: **double-click the row** to open the editor of that record; the dossier refreshes after you save. A single click does nothing, and the rows are **not** links to other people: to move to somebody else, use the tree, *Jump to* or *Find record*.
- A row can show a grey subtitle, an evidence badge on the right (such as *orig/prim/dir*, for original, primary, direct), a padlock for a restricted or confidential record, and, for conclusions, a coloured square for the proof status: **green** proven, **blue** supported, **yellow** conflicting evidence, **red** disproven.
