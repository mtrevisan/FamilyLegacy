# Group timeline, group network and group career

Three windows show the life of a group, or the groups of a person, over time. They all open in a modal dialog with a single **Close** button (`Esc` also closes it). They are for looking: a click does not move the main window.

## Group Timeline

**Group > Group Timeline...** (it needs a selected group) draws the group and the people and groups connected to it on a time axis.

**The rows**, from the top:

1. the group itself, with its own events and attributes, and the historical context;
2. the groups it is part of;
3. its subgroups;
4. its members.

Members and subgroups are sorted by the earliest start date, then by name; the ones without dates come last.

**The bars.** Each membership is a bar from the *Valid From* to the *Valid To* of its relationship. The label is the role (or the type of the relationship if there is no role). A relationship that has ended is fainter and dashed and its label adds *(ended)*. A membership with no end is still in force, so its bar ends **today** and never goes on beyond it, however far you zoom out: if it has a start its label adds *(ongoing)*; if it has no dates at all it is kept, begins at the left edge of the window and says *(dates unknown)*. The exception is a membership marked as ended but with no end date, whose end stays open, because it is not known. A person who was a member several times has all the bars on one row.

**The axis** runs from the earliest date of the bars to the latest one, or to today if some bar has no end. If all the bars are undated, the last 100 years are shown.

**The controls**

- **Any year**, **Year**: with *Any year* unticked, the spinner shows only the bars whose dates cover that year. The bars with no known date stay, and the rows left empty disappear.
- **Context**: shows or hides the bands of historical context behind the bars.
- **Zoom** (`−`, `+`, `Fit`); `Ctrl` + the wheel zooms at the cursor, a drag with the left button moves along the axis, the plain wheel scrolls up and down.
- The **Roles** legend at the bottom has a coloured check box for each role (and *(no role)*), to show or hide the bars of that role, and **Show all**.

**Mouse.** One click selects a bar or a row label; a double-click opens the editor of the record behind it (the relationship, the member or the group) and redraws the chart if you save. The tooltip of a bar gives its label, type, role and dates.

Records hidden by the [privacy](privacy.md) policy are left out.

## Group Network

**Group > Group Network...** draws the groups related to the selected one as a network of boxes.

- It follows only the relationships **between groups**: *part of* and *associate*, in either direction. A relationship with dates that do not cover the chosen year is dropped; undated ones are kept.
- The **wholes are above their parts** (the level comes from *part of*). A group with no *part of* takes the level of its nearest associated group.
- A **thick line** is *part of*; a **dashed line** is *associate*. Each box has a badge with the **number of members**, and the centre group is highlighted. The legend at the bottom explains these.

**The controls**

- **Levels** (default 2, from 0 to 6): how many relationships away from the centre group to go.
- **Part of**, **Associated** (on by default) and **Members of the subgroups** (off by default): what to draw and follow. With the last one ticked, the people of every subgroup are added to the count of the group, each counted once.
- **Any year** and **Year**: as in the timeline.
- Zoom: `−`, `+`, `⤢` (fit); `Ctrl` + the wheel also zooms.

**Mouse.** One click highlights a box; a **double-click makes that group the new centre**; a drag moves the picture. You cannot edit from this window. The tooltip of a box shows the name, the number of members and the identifier; the tooltip of a line says that A is part of B, or is associated with B, with the role and the dates if present.

## Group Career

**Individual > Group Career...** (it needs a selected individual) shows the groups a person belonged to, against their own life.

- The first row is the person's **life**: events such as the birth, the marriage and the death, attributes such as the occupation, and the context bands.
- Then one row for **each group** the person was a member of, the earliest first, with a bar for each period. The bars are coloured by role; a person who left and came back, or changed role, has more than one bar on the row.
- A membership with a missing start begins at the person's birth, and one with a missing end stops at the death (or at the earliest or latest known date of the person if the birth or the death is not known). A membership with no dates at all spans the whole life and reads *(dates unknown)*.
- The view is empty if the person has no memberships and no dated events or attributes.

The year filter, the role legend, the double-click to edit and the zoom work as in the timeline.

## Jurisdiction Timeline

**Place > Jurisdiction Timeline...** shows how a place changed jurisdiction in time. The place is the selected one if the selection is a place, otherwise you are asked to pick one. It works like the group timeline, and it has the same year control, **Context** box and zoom.

**The rows**, from the top:

1. the place itself, with the historical context that touches it (if any);
2. the places it is related to: the ones it is part of (administratively, ecclesiastically, judicially, geographically, for the land registry) or that it succeeded;
3. the places that are related to it the other way round (the ones that are part of it, or that succeeded it); their bars say *(incoming)*.

**The bars.** Each place relationship is a bar from its *Valid From* to its *Valid To*. The colour and the label are the type of the relationship, for example *ecclesiastical_part_of*, so a parish that passed from one diocese to another has two bars, one after the other, on two rows. The legend at the bottom hides a type. A relationship with no end is still in force: its bar ends **today** and never goes on beyond it, however far you zoom out. If it has a start it says *(ongoing)*; if it has no dates at all it is kept, begins at the left edge of the window and says *(dates unknown)*. A double-click opens the editor of the relationship, or of the place if you click its name.

Records hidden by the [privacy](privacy.md) policy are left out.
