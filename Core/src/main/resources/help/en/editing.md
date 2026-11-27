# Editing people, groups and places

## Opening an editor

- Select an individual and press `F2`, or double-click its box, or choose **Individual > Edit Individual...**.
- For a group, use **Group > Edit Group...**.

An editor is a dialog with several tabs. The *Properties* tab holds the fields of the record itself; the other tabs list the records connected to it. For an individual they are: attributes, relationships, participations (the events the person took part in), context, research, sources, notes, privacy and audit.

Fields marked with an asterisk are required. **OK** checks the fields that are really required and tells you which one is missing, and `Esc` closes the editor and discards the changes. What every field means is explained in [The record editors](editors.md) and the pages that follow it.

## Adding people

- **Individual > Add Individual...** creates a new individual. Its window opens with a **Quick entry** (full name, sex, birth, death, burial and occupation) for the common facts: see [People and groups](editors-people.md#individual).
- **Individual > Add Partner...**, **Add Parent...** and **Add Child...** create a new individual and link it to the selected one (the type of the relationship is asked, except for **Add Parent...**: the parent is a *biological* one, unless you choose the menu item holding `Shift`, which asks the type; it can also be changed later in the relationship). When the person is a child (new, or existing with **Connect Child...**) and the selected individual has partners, the same window also asks who the other parent is: one of the partners (preselected), another individual, or nobody (while both parents are *biological children* links and the sex of the selected one is known, only individuals of the opposite sex are offered, and the search and creation windows have that sex fixed; with the generic *child* or other kinds of link any sex is accepted); each parent has its own relationship type; **Connect Partner...**, **Connect Parent...** and **Connect Child...** do the same with an existing person. **Paste as Partner**, **Paste as Parent** and **Paste as Child** move the person copied with **Relocate**. For a partner the same window also asks when and where the union began, and makes the marriage (or engagement) event together with the relationship: see [Relationships](relationships.md).

How the links between people are recorded, and the shortcuts of the tree (**Connect > Child...**), are explained in [Relationships](relationships.md).

## Quick Entry of a family

**Individual > Quick Entry...** (`Ctrl+Shift+N`) is the fastest way to enter a small family: one window for a person, the parents, the partner, the union and the children.

- **Person**: the individual selected in the view (the box that says *The selected individual*), or, if you clear it or nothing is selected, a new person typed in the same way as the others.
- **Parents**: the father and the mother of the person, each with the full name, the sex (a father is a man and a mother a woman unless you choose otherwise), the birth and the death, and its own **Relationship** (*biological child* by default, so that each parent can be, for instance, an adoptive one). It is the quick way to enter a person from the bottom of the tree up. Two parents of the same sex cannot be the *biological* parents: choose another type.
- **Partner**: the full name, the sex, the birth and the death (date and place) of the partner. The **Relationship** is *spouse* by default (the kinds of spouse begin with a *marriage* event, the *engaged partner* with an *engagement*, the *cohabiting partner* with none); the **Date** and the **Place** of the union are asked only for a partner and only for the types that have an event, and make that event with both people as its *principal* participants.
- **Children**: the type of the relationship (*biological child* by default) and, for each child, the full name, the sex, the birth and the death; **Add a child** and **Remove** change the rows. Each child is a child of the person and, if there is a partner, of the partner too. Two parents of the same sex cannot have *biological* children: choose another type.

Each name is split into the parts of a name as in the [quick entry of an individual](editors-people.md#individual), and what it will be is shown under the field. **Fill only what you know**: a person with nothing in it is not created, and everything can be completed later by editing the people. The window does nothing until **OK**; **Esc** closes it without creating anything. When it is done, a line at the right end of the tool bar says what was created (*Quick entry created 4 people, 3 events, 1 place, 5 relationships*) for a few seconds. Everything the quick entry created is **one step** of the history: **Edit > Undo** (`Ctrl+Z`) takes back the people, their events, the places and the relationships together (it asks first, as for every change made from a window), and **Redo** puts them back; **Edit > Recent Changes...** lists the records it added. A place that the file already has with the same name is not used silently: you are asked which one, as everywhere else. A place may be typed with the places that contain it (*Padova, Veneto, Italy*), as in the [quick entry of an individual](editors-people.md#individual).

## Changing a date of birth

A date of birth is not a field of the individual: it belongs to the **birth event**. When you create a *new* individual you can type the birth and the death in the Quick entry of **Add Individual...** and the events are made for you. To change one of an existing individual, the quickest way is the **Quick entry** at the top of **Edit Individual...** (*Sex, birth and death*), which changes the date and the place in place when they are not cited (see [People and groups](editors-people.md#individual)). The longer way, which works for any event and for the cited ones:

1. Open **Edit Individual...** and go to the **Participations** tab (*Event Participations*).
2. Select the participation in the birth event (the role is *principal*) and open it. The **Event** field of the participation names the event.
3. Open that event. In its *Properties* tab, change **Date** (the same tab holds the type, description, place and confidence).
4. Save the event, then the participation if you changed it.

A shorter way is **Event > Manage Events...**: it lists every event with its date, its place and the people who took part, and you can search for the birth (by the name of the person, the place or the date) and edit it directly. In the sidebar, the **Events** section of the individual shows the same events.

Two cautions:

- An event is shared by everyone who took part in it. If it has several participants, changing its date changes it for all of them, which is what you want for a birth, a marriage or a burial.
- If two sources disagree about the date, do not overwrite one with the other. Record each assertion with its own source and settle the matter with a conclusion: see [Research](research.md).

## Moving, deleting and unlinking

- **Relocate** (`Ctrl+Shift+M`) only copies the selected individual or group to a clipboard. **Paste as > Partner**, **Parent** or **Child**, in the context menu of a person, then detaches the person from all their current links and reattaches them in the new place. See [Relationships](relationships.md#moving-and-removing-links) before using it.
- **Delete** (`Del`) removes the selected individual together with every relationship that involves it, and its event participations and attributes. You are asked to confirm, and the dialog tells you how many relationships will go.
- **Unlink Relationships...** opens a dialog that lists the relationships of the selected individual or group and removes the ones you choose, without deleting the people.
- **Set as Root** makes the selected individual the root of the current view.

Every change can be undone with `Ctrl+Z`; **Edit > Recent Changes...** lists what changed.

## Groups

A group is a set of individuals (a family, a household, a congregation...). Membership is stored as relationships, so a group can have members with roles, and a group can be part of a larger one.

- **Group > Add Group...** and **Add Members...** create a group and add people to it. **Add Group...** is available only in the ego network view, the one that can show a group as its root.
- **Group Timeline...** and **Group Network...** show what the members did and how they are connected.
- **Group > Delete**, **Unlink Relationships...** and **Set as Root** work as for individuals. Adding a member is described in [Relationships](relationships.md#a-member-of-a-group-with-a-role).

## Individual helpers

- **Kinship Calculator...** tells how two individuals are related; the selected individual fills the first side. See [Kinship and consanguinity](kinship.md).
- **Memberships Over Time...** shows the groups the individual belonged to in time, with the role in each. See [Group timeline, group network and memberships over time](group-views.md).
- To merge two entries for the same person, see [Finding duplicates and merging people](merging.md).

## Places

- **Place > New Place...** creates a place from one line, with the places that contain it (see [New Place](editors-events-places.md#new-place-one-line-with-the-places-that-contain-it)); **Manage Places...** lists and edits them (see [Manage dialogs](searching.md#manage-dialogs)); **Delete** removes the selected one.
- Places form a hierarchy (a town in a province in a country), and a place can sit under more than one parent.
