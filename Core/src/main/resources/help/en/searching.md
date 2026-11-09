# Jump to, Find record and bookmarks

## The toolbar and the history

From the left, the toolbar has **◄** and **►** (back and forward in the history, `Ctrl+Left` and `Ctrl+Right`; they do nothing while the history is empty), **Jump To...** (`Ctrl+J`) and **Edit...** (`F2`, edits the selected person or group in whichever view is active). Then come the four view buttons and **Sidebar**. **View > Show Toolbar** hides the whole toolbar. There is no button for *Find record*: use **Navigate > Find record...** (`Ctrl+F`).

**Navigate > Clear History** forgets the roots you have visited (the entry is grey afterwards: there is no message). **Back** and **Forward** skip a record that has been deleted since you visited it.

## Jump to individual (`Ctrl+J`)

It searches **individuals** by name and **re-roots** the current view on the one you choose. In the **Ego Network** view it also finds groups, and the menu entry reads *Jump to individual/Group...*; a *Type* list then appears.

## Find record (`Ctrl+F`)

It searches **any type of record**: individual, group, event, event participation, place, source, repository, media, relationship, place relationship, conclusion, identity hypothesis, research question, research activity, research task, historic event and cultural norm. What happens when you choose one:

- an **individual**: the view is re-rooted on it;
- a **group**: the view is re-rooted if the Ego Network is active; in the other views the editor of the group opens;
- **any other record**: its editor opens (the views are rebuilt if you save).

## The search dialog

Both open the same dialog, *Select Record*:

- **Type**: only when more than one type is searchable.
- **Search text**.
- **Whole word** and **Fuzzy**, both off.
- A box of **filters** for the type. For individuals: *Sex* (the preferred sex of the person; a person whose sex is undetermined is not excluded), *Event Type*, *Date From* and *Date To* (each with its calendar), and *Place*. For groups: *Name* and *Type*. The filters on events, dates and places apply to the events of the individual.
- **Results**: the list, with a status line (*Found N record(s)*) and a progress bar while it searches.
- Buttons **Edit...**, **Unlink** and **Delete** (on the selected result), and **Select** and **Cancel**.

The search starts by itself about 0.4 seconds after you stop typing or change a filter, and runs in the background. `Esc` stops a search in progress and, if none is running, closes the dialog. A double-click on a result is **Select** (in the *Manage* dialogs it is **Edit**: see [Manage dialogs](#manage-dialogs)). Records hidden by the [privacy](privacy.md) policy are not found.

**What the text is matched against.** The *display text* of the record. For an individual it is the **first name** and the identifier, such as `Bortolo Gallinaro [I12]`: other names, dates and places are not searched by the text (use the filters for them). The text is split into words; each word is looked for in the words of the record, in any order (`gall bor` finds `Bortolo Gallinaro`). A record qualifies if **at least one** word matches, and the records that match more words come first, then by similarity, then alphabetically.

**Places** are also found by the places that contain them: the text (and the *Name* filter) of a *place* search is matched against the names of the place and against the names of every place it is part of, through any relationship (administrative, ecclesiastical, ...), so `Treviso Veneto` finds Treviso, which is part of Veneto, and `Veneto` finds every place in Veneto. Events were already found by the complete name of their place.

- By default a word matches a **part of a word**, ignoring capitals.
- **Whole word**: a word must match a complete word.
- **Fuzzy**: a part of a word, or a small spelling difference (up to one letter for every three letters of the word).
- Both together: a fuzzy match, on whole words only.
- An empty text lists everything that passes the filters.

The result list shows, for individuals, the sex (`[M]`, `[F]`, `[U]`), the name, the identifier, and the birth and death date and place.

**Events and participations.** The text of an event is its type, its date, its complete place and the people who took part, so the text finds an event by any of them (see [Events and dates](events-and-dates.md#events)). The text of a participation is the participant, the role and the event (type, date and place).

## Manage dialogs

**Manage Events...**, **Manage Participants...**, **Manage Sources...**, **Manage Repositories...** and **Manage Places...** use the same dialog as *Find record*, with the filters of the type, but they manage records and do not pick one:

- there is no **Select** button, and **Cancel** is called **Close** (`Esc` closes the dialog, or stops a search in progress);
- a double-click on a record **edits** it;
- **New...** creates a record, and the dialog stays open with the new record in the list;
- **Edit...**, **Unlink** and **Delete** work on the selected record. **Unlink** lists the records that refer to it and removes the ones you choose.
- in **Manage Places...** the details of the selected place (on the right of the hierarchy) end with **Linked to**: the events, attributes, repositories... that use the place, so you can see what depends on it before you unlink or delete it.

### Search fields in the lists and boards

The search field of **Manage Media**, **Event Types**, **Research Log**, **Research Board**, **Context Board**, **Find Duplicates** and **Citation Analysis** works as the one of *Find record*: the text is matched word by word, a row with more of the words ranks higher, and the check boxes next to the field narrow the search to **Whole word** or widen it to typos (**Fuzzy**). In the list of uncited assertions of **Citation Analysis**, **Search in notes and citations** looks in the notes too, and the best matches come first.

## Bookmarks

**Bookmarks** saves a view so that you can return to it.

- **Bookmark current view...** (`Ctrl+D`) asks for a name; a name of spaces is not accepted. If nothing is loaded it says there is no view to bookmark; in a document that has no file yet it asks you to choose a file first, because a bookmark belongs to a file. If the file already has a bookmark with that name you are asked whether to replace it: two bookmarks with one name could not be told apart.
- The menu lists the bookmarks of **the open file**, oldest first; the tooltip of each gives its view and the **name** of the root.
- **Manage bookmarks...** lists them with the name and the view and root (by name). You can **Rename...** (to a name not used by another bookmark of the file) and **Delete** (with confirmation). There is no reordering, no export and no *go to* button: apply a bookmark from the menu.

**What is stored**: the name, the **type of view** (tree, Sugiyama, ego network, chronomap), the **identifier of the root record** and the creation time. For the Sugiyama graph, the *show partner* setting is stored too. **Also stored**: the file the bookmark was made in. **Not stored**: the zoom and scroll position, the selected box, the date of the chronomap, filters, the layout and the state of the sidebar.

Bookmarks are kept in one place for all your genealogies (`~/.familylegacy/bookmarks.properties`, written through a temporary file, so that a crash does not damage it), but each one **belongs to the file it was made in**, because it refers to the identifier of a record and the same identifier is another person in another file: in a different file it is not listed. A bookmark made by an older version, that is tied to no file, belongs to none and is not listed (it stays in the bookmarks file). A bookmark whose root has been deleted is not applied: a message says so. Bookmarks whose root is hidden by the current privacy policy are not listed.
