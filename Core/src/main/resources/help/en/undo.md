# Undo, redo and recent changes

## Undo and redo

**Edit > Undo** (`Ctrl+Z`) and **Edit > Redo** (`Ctrl+Y`, or `Ctrl+Shift+Z`) step back and forward through your changes. The two items are grey when there is nothing to undo or to redo. On macOS they are `Cmd+Z` and `Cmd+Shift+Z`.

- **What a step is.** The application keeps a copy of the whole file after each action. A step is closed when you start the next action (a click, a key, a menu), or when the last dialog you used closes. Record windows opened one from the other, such as a name inside a person, that you save one after the other, count as **one** step.
- **How far back.** Up to **100** steps; the older ones are dropped. A new change after an undo discards what you could have redone.
- **What is covered.** Any change to the data: edits from the views and the menus, adding, editing and deleting in the record editors, the header, and the tools that change the file, such as a merge. **Not covered:** the preferences, the bookmarks, the [tags](editors.md#tags) of the records, the view settings, the current root and selection, and the files on disk.
- **Saving does not erase the history**: every change is written to the file by itself, and you can still undo it. An undo or a redo is written to the file like any other change.
- If the step to undo comes from a record window that has since been closed, you are asked *The last change was saved from a record window that has since been closed. Undo it anyway?*

**The history is cleared, and cannot be undone**, whenever the whole file is replaced: **File > New**, **File > Open** (also from the recent files), **File > Import** and **Restore from Backup**. Nothing can undo an import or a restore. Make a copy first with **File > Backup...**.

The history lives in memory only and is lost when you close the application.

## Recent Changes

**Edit > Recent Changes...** opens a window (which does not block the application) that lists the changes of this session, **most recent first**, one row for each record touched:

| Column | Meaning |
|---|---|
| **Time** | When the change was made. |
| **Change** | *Added*, *Modified* or *Removed*. |
| **Type** | The type of the record. A change of the header is shown as *Header*. |
| **Record** | The name of the record as it was at the time. |
| **Status** | *Applied*, or *Undone* (an undone step stays in the list until a new change discards it). |

Above the list, **Record type** filters the rows (*All types*), and a line says how many changes there are. The buttons:

- **Show changes**: a side-by-side view of the record before and after.
- **A double-click** opens the editor of the record. It is not available for a removed record, and warns if the record is no longer in the file.
- **Refresh**. The window also refreshes when you come back to it.

### Reverting an older step

Undo only goes back one step at a time, newest first. **Revert step** undoes **one** step of the list on its own, whichever it is, and leaves the later ones as they are: every record the step touched goes back to how it was before it (an added record is removed, a removed one is put back, a modified one is restored). Select a row and press the button; the **whole step** is reverted, not only the record of that row.

- If nothing the step touched has changed since, you are asked to confirm, and the revert is exact.
- If some of those records were **changed again afterwards**, the window lists them and warns you: reverting overwrites those later changes, and other records that refer to them may no longer be consistent. It is done only if you confirm (a *forced* revert).
- The revert is a **new step** of its own: you can undo it with **Edit > Undo**.
- It is not available for a step that has been undone (use **Redo**), nor for one that has left the history.

**This record only** (next to the type filter) shows only the changes of the record selected in the list, as its history; press it again to show all the changes. **Show changes** highlights, field by field, what differs between the record before and after.
