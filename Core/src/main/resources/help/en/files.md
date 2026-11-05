# Files

The **File** menu holds everything that concerns the whole document.

## Creating, opening and saving

**There is nothing to save by hand.** Every change you make is written to the file of the document by itself, a fraction of a second after you make it (and the same for an undo and a redo): the file is always what you see, so there is no *Save* command and no question about unsaved changes. If the file cannot be written (the disk is full, the file is locked) a message says so once, and the application tries again at the next change.

- **New file...** (`Ctrl+N`) asks where to create the file of the new document, writes it empty and opens it. If you cancel, nothing changes.
- **Open file...** (`Ctrl+O`) opens a `.flef` file. While the file is read, a progress window is shown; if the file cannot be read, or is not a valid FLEF file, an error says so.
- **Open Recent** lists the files opened lately (the last ten, the most recent first). A file is in the list once, however its path is written; two different files with the same name are shown with their folder. **Clear list** empties it. A file that is not there any more (moved, renamed or deleted) is not opened: a message says so and asks whether to **remove it from the list**.
- **Save as...** (`Ctrl+Shift+S`) writes the document to another file, which **becomes the file of the document**: the changes are saved to it from now on, and the previous file stays as it was last written. Use it to make a branch of your work (for a copy that does not change the document, use **Backup...**). The `.flef` extension is added if you do not type it, and you are asked before an existing file is overwritten.
- **File Properties...** shows the file (when saved), its size, its last modification date, whether it can be read and written, its encoding and the statistics of what it contains (the number of records, and the number for each type). The text is in the language of the application and can be copied to the clipboard.
- **Exit** (`Ctrl+Q`) closes the application. The close button of the window does the same. Before closing, anything still waiting to be written is written, without asking.

Before an operation that replaces the document or closes the application (New, Open, Open Recent, Exit or the close button of the window, Import, Restore) what is still waiting is written, so nothing is lost. If that write fails you are told, and asked whether to go on anyway (the changes would be lost); answering *No* stops the operation.

**The one document with no file** is the empty one the application starts with. At your first change you are asked where to write it, and from then on it is saved like any other. If you cancel that choice the document stays in memory only; when you close it, or open another file, you are asked whether to choose a file (*Save*), lose the changes (*Discard*) or give up (*Cancel*).

A file can be plain text or compressed (the application recognises it), and is always read as UTF-8. **Saving is safe against interruptions**: the data is written to a temporary file in the same folder, forced to the disk, and only then moved over the real file, so a full disk, a crash or a locked file leave the previous version intact (the temporary file is removed). Because every change is written over the file, backups are the way to keep older versions: see [When something goes wrong](troubleshooting.md#before-anything-else).

## Importing from other programs

**File > Import** converts data from another format into a new FLEF file:

- **GEDCOM** (versions 5.5, 5.5.1 and 7; the version is read from the file itself).
- **Gramps XML**.

The import never merges into the open document. After the conversion you choose where to save the new FLEF file; it is written and then opened as the current document. If you cancel that choice, the import is discarded and nothing changes.

**The import replaces the open document**, whose changes are written first, as with New and Open. What is kept, converted and dropped for each structure is in [Importing from GEDCOM and Gramps](import.md).

## Backup and restore

- **Backup...** saves a copy of the document to a file you choose, with a progress window, as Save does. The name is pre-filled with the date and time, so successive backups do not overwrite each other. A backup does not change the open document, and it is written as it is, without validation: if the document has errors they are kept, which makes a backup useful to recover from a bad edit.
- **Restore from Backup...** loads a FLEF file (plain or compressed, as with Open; a progress window is shown while it is read) and *replaces the open document with it*. You are asked to confirm, and the confirmation says that the content of **the file of the open document** is replaced too, because the restored data is saved like every change: make a backup of the current content first if you may need it. This cannot be undone, and the undo history is cleared. A document that has no file is asked where to be written.

## Checking a file

**Tools > Validate File...** runs the full FLEF validation on the document and lists the problems, each one linked to the record it concerns. See [Tools](tools.md#validate-file).

## Privacy

Records can carry a privacy setting, and **View > Privacy** decides what the views show. See [Navigating the tree](navigating.md#privacy).
