# Jump to, Find record and bookmarks

## The toolbar and the history

From the left, the toolbar has **◄** and **►** (back and forward in the history, `Ctrl+Left` and `Ctrl+Right`; they do nothing while the history is empty), **Jump To...** (`Ctrl+J`) and **Edit...** (`F2`, edits the selected person or group in whichever view is active). Then come the four view buttons and **Sidebar**. **View > Show Toolbar** hides the whole toolbar. There is no button for *Find record*: use **Navigate > Find record...** (`Ctrl+F`).

**Navigate > Clear History** forgets the roots you have visited.

## Jump to individual (`Ctrl+J`)

It searches **individuals** by name and **re-roots** the current view on the one you choose. In the **Ego Network** view it also finds groups, and the menu entry reads *Jump to individual/Group...*; a *Type* list then appears.

## Find record (`Ctrl+F`)

It searches **any type of record**: individual, group, event, place, source, repository, media, relationship, place relationship, conclusion, identity hypothesis, research question, research activity, research task, historic event and cultural norm. What happens when you choose one:

- an **individual**: the view is re-rooted on it;
- a **group**: the view is re-rooted if the Ego Network is active; in the other views the editor of the group opens;
- **any other record**: its editor opens (the views are rebuilt if you save).

## The search dialog

Both open the same dialog, *Select Record*:

- **Type**: only when more than one type is searchable.
- **Search text**.
- **Whole word** and **Fuzzy**, both off.
- A box of **filters** for the type. For individuals: *Sex*, *Event Type*, *Date From* and *Date To* (each with its calendar), and *Place*. For groups: *Name* and *Type*. The filters on events, dates and places apply to the events of the individual.
- **Results**: the list, with a status line (*Found N record(s)*) and a progress bar while it searches.
- Buttons **Edit...**, **Unlink** and **Delete** (on the selected result), and **Select** and **Cancel**.

The search starts by itself about 0.4 seconds after you stop typing or change a filter, and runs in the background. `Esc` stops a search in progress and, if none is running, closes the dialog. A double-click on a result is **Select**. Records hidden by the [privacy](privacy.md) policy are not found.

**What the text is matched against.** The *display text* of the record. For an individual it is the **first name** and the identifier, such as `Bortolo Gallinaro [I12]`: other names, dates and places are not searched by the text (use the filters for them). The text is split into words; each word is looked for in the words of the record, in any order (`gall bor` finds `Bortolo Gallinaro`). A record qualifies if **at least one** word matches, and the records that match more words come first, then by similarity, then alphabetically.

- By default a word matches a **part of a word**, ignoring capitals.
- **Whole word**: a word must match a complete word.
- **Fuzzy**: a part of a word, or a small spelling difference (up to one letter for every three letters of the word).
- Both together: a fuzzy match, on whole words only.
- An empty text lists everything that passes the filters.

The result list shows, for individuals, the sex (`[M]`, `[F]`, `[U]`), the name, the identifier, and the birth and death date and place.

## Bookmarks

**Bookmarks** saves a view so that you can return to it.

- **Bookmark current view...** (`Ctrl+D`) asks for a name. If nothing is loaded it says there is no view to bookmark.
- The menu lists the bookmarks, oldest first; the tooltip of each gives its view and root.
- **Manage bookmarks...** lists them with the name and the view and root. You can **Rename...** and **Delete** (with confirmation). There is no reordering, no export and no *go to* button: apply a bookmark from the menu.

**What is stored**: the name, the **type of view** (tree, Sugiyama, ego network, chronomap), the **identifier of the root record** and the creation time. For the Sugiyama graph, the *show partner* setting is stored too. **Not stored**: the file, the zoom and scroll position, the selected box, the date of the chronomap, filters, the layout and the state of the sidebar.

Bookmarks are kept in **one file for all your genealogies**, not one for each file, and refer to the identifier of the record. A bookmark is therefore applied to whatever file is open, and is meaningful only if that file has the same identifier. Bookmarks whose root is hidden by the current privacy policy are not listed.
