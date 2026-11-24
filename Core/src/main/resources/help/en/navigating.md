# Navigating the tree

## The views

The **View > Projection** menu (and the toolbar buttons) switch between four views of the same data. The person selected in one view is the starting point of the others.

| View | Shortcut | What it shows |
|---|---|---|
| **Family Tree** | `Ctrl+1` | A classical pedigree: one box per individual, couples side by side. |
| **Sugiyama Graph** | `Ctrl+2` | The same pedigree as a layered graph. Each person appears once, so endogamy and pedigree collapse show up as converging lines instead of repeated boxes. |
| **Ego Network** | `Ctrl+3` | The relations of one individual or group, drawn as a graph. |
| **Chronomap** | `Ctrl+4` | The individuals on a map and in a list, filtered by a timeline: where and when things happened. |

In the tree, `Ctrl+L` toggles the layout between vertical and horizontal, and `Ctrl+T` shows or hides the lifespans strip at the bottom. `F11` (or **View > Full Screen**) puts the window in full screen: it covers the whole screen, task bar included, with no title bar; the menus stay. `F11` again brings the window back as it was. Going in or out of full screen closes the windows that are open beside the main one, such as Recent changes. The three family views are described in [The tree, the graph and the ego network](views.md), and the map in [The Chronomap](chronomap.md).

**View > Show Toolbar** and **View > Show Sidebar** hide or show those parts of the window. From the *Standard* level, **View > Show Tasks Panel** shows the research tasks still to be done below the views (see [The tasks panel](research.md#the-tasks-panel)).

## Moving around

- **Click the name** of a person in a box to make that person the new root of the view. The arrow keys move the selection and `Enter` re-roots on the selected box.
- **Navigate > Jump to individual...** (`Ctrl+J`) searches people by name; in views that also show groups it reads *Jump to individual/Group...*.
- **Navigate > Find record...** (`Ctrl+F`) finds any record, not only people. Both are explained in [Jump to, Find record and bookmarks](searching.md).
- **Back** and **Forward** (`Ctrl+Left`, `Ctrl+Right`) walk through the roots you have visited. **Clear History** forgets them.
- **Right-click** a box to open a context menu with the commands that apply to it.

## The sidebar

The sidebar (the *Dossier*) lists everything recorded about the selected individual or group, in collapsible sections such as **Identity**, **Events** and **Sources**. A click on a row opens the editor of the record behind it, on the right tab and with the focus on the right item. The sections are described in [The tree, the graph and the ego network](views.md#the-sidebar).

## Bookmarks

**Navigate > Bookmarks** saves the current view, that is the active view plus the current root, so that you can come back to it later. **Bookmark current view...** (`Ctrl+D`) adds one and **Manage bookmarks...** renames or deletes them. What is stored, and why a bookmark is not tied to one file, is in [Jump to, Find record and bookmarks](searching.md#bookmarks).

## Privacy

**View > Privacy** chooses what is shown, according to the privacy set on each record:

- **Show All**
- **Hide Confidential**
- **Hide Restricted and Confidential**

This is a view preference: it does not change or delete any record. How the level of a record is decided, what counts as a living person, and what the policies hide in each place are explained in [Privacy](privacy.md).

## Order of children, partners and dossier

**View > Order of Children, Partners and Dossier** decides how the views sort them:

- Children: by birth date (oldest first), alphabetically, or as stored in the file.
- Partners: by date of the union (earliest first), alphabetically, or as stored in the file.
- Dossier events and dossier attributes (of individuals and of groups), each with its own choice: oldest first (the default), newest first, or as stored in the file. Rows without a usable date always come last. The relationships of the dossier are always oldest first; sources and notes keep their grouping.

Like privacy, this is only a way of looking at the data: no record is touched.
