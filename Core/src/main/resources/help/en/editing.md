# Editing people, groups and places

## Opening an editor

- Select an individual and press `F2`, or double-click its box, or choose **Individual > Edit Individual...**.
- For a group, use **Group > Edit Group...**.

An editor is a dialog with several tabs. The *Properties* tab holds the fields of the record itself; the other tabs list the records connected to it. For an individual they are: attributes, relationships, participations (the events the person took part in), context, research, sources, notes, privacy and audit.

Fields marked with an asterisk are required. **Save** checks the fields that are really required and tells you which one is missing, and `Esc` closes the editor and discards the changes. What every field means is explained in [The record editors](editors.md) and the pages that follow it.

## Adding people

- **Individual > Add Individual...** creates a new individual.
- **Individual > Add Child...** opens the editor of the selected individual on the **Relationships** tab, where you add the relationship that makes the new person a child of this one.

How the links between people are recorded, and the shortcuts of the tree (**Connect Individual...**, **Connect Child...**), are explained in [Relationships](relationships.md).

## Changing a date of birth

A date of birth is not a field of the individual: it belongs to the **birth event**. To change it:

1. Open **Edit Individual...** and go to the **Participations** tab (*Event Participations*).
2. Select the participation in the birth event (the role is *principal*) and open it. The **Event** field of the participation names the event.
3. Open that event. In its *Properties* tab, change **Date** (the same tab holds the type, description, place and confidence).
4. Save the event, then the participation if you changed it.

A shorter way is **Event > Manage Events...**: it lists every event with its date, its place and the people who took part, and you can search for the birth (by the name of the person, the place or the date) and edit it directly. In the sidebar, the **Events** section of the individual shows the same events.

Two cautions:

- An event is shared by everyone who took part in it. If it has several participants, changing its date changes it for all of them, which is what you want for a birth, a marriage or a burial.
- If two sources disagree about the date, do not overwrite one with the other. Record each assertion with its own source and settle the matter with a conclusion: see [Research](research.md).

## Moving, deleting and unlinking

- **Relocate** (`Ctrl+X`) only copies the selected individual or group to a clipboard. **Paste**, in the context menu of an empty box of the tree (the entry reads *Paste ... Here*), then detaches the person from all their current links and reattaches them in the new place. See [Relationships](relationships.md#moving-and-removing-links) before using it.
- **Delete** (`Del`) removes the selected individual together with every relationship that involves it, and its event participations and attributes. You are asked to confirm, and the dialog tells you how many relationships will go.
- **Unlink Relationships...** opens a dialog that lists the relationships of the selected individual or group and removes the ones you choose, without deleting the people.
- **Set as Root** makes the selected individual the root of the current view.

Every change can be undone with `Ctrl+Z`; **Edit > Recent Changes...** lists what changed.

## Groups

A group is a set of individuals (a family, a household, a congregation...). Membership is stored as relationships, so a group can have members with roles, and a group can be part of a larger one.

- **Group > Add Group...** and **Add Members...** create a group and add people to it.
- **Group Timeline...** and **Group Network...** show what the members did and how they are connected.
- **Group > Delete**, **Unlink Relationships...** and **Set as Root** work as for individuals. Adding a member is described in [Relationships](relationships.md#a-member-of-a-group-with-a-role).

## Individual helpers

- **Kinship Calculator...** tells how two individuals are related; the selected individual fills the first side. See [Kinship and consanguinity](kinship.md).
- **Group Career...** shows the groups the individual belonged to in time, with the role in each. See [Group timeline, group network and group career](group-views.md).
- To merge two entries for the same person, see [Finding duplicates and merging people](merging.md).

## Places

- **Place > New Place...** creates a place; **Manage Places...** lists and edits them (see [Manage dialogs](searching.md#manage-dialogs)); **Delete** removes the selected one.
- Places form a hierarchy (a town in a province in a country), and a place can sit under more than one parent.
