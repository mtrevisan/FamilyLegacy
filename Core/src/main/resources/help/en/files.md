# Files

The **File** menu holds everything that concerns the whole document.

## Creating, opening and saving

- **New file...** (`Ctrl+N`) starts an empty document.
- **Open file...** (`Ctrl+O`) opens a `.flef` file. While the file is read, a progress window is shown; if the file cannot be read, or is not a valid FLEF file, an error says so.
- **Open Recent** lists the files opened lately. **Clear list** empties it.
- **Save as...** (`Ctrl+Shift+S`) writes the document to a file you choose. The `.flef` extension is added if you do not type it, and you are asked before an existing file is overwritten.
- **File Properties...** shows the file (when saved), its size, its last modification date and the statistics of what it contains. The text can be copied to the clipboard.
- **Exit** (`Ctrl+Q`) closes the application.

Before an operation that replaces the document (New, Open, Exit, Import) you are asked whether to **save**, **discard** or **cancel**, **if the document has unsaved changes**. The application compares the document with what is on the file, so an edit that you undo or revert by hand does not count, and a document you have just opened or saved is not asked about. Cancelling, or a save that fails, stops the operation.

A file can be plain text or compressed (the application recognises it), and is always read as UTF-8. **Saving is safe against interruptions**: the data is written to a temporary file in the same folder, forced to the disk, and only then moved over the real file, so a full disk, a crash or a locked file leave the previous version intact (the temporary file is removed). Backups are still the way to keep older versions: see [When something goes wrong](troubleshooting.md#before-anything-else).

## Importing from other programs

**File > Import** converts data from another format into a new FLEF file:

- **GEDCOM** (versions 5.5, 5.5.1 and 7; the version is read from the file itself).
- **Gramps XML**.

The import never merges into the open document. After the conversion you choose where to save the new FLEF file; it is written and then opened as the current document. If you cancel that choice, the import is discarded and nothing changes.

**The import replaces the open document**, so before it starts you are asked whether to save the changes, discard them or cancel, as with New and Open. What is kept, converted and dropped for each structure is in [Importing from GEDCOM and Gramps](import.md).

## Backup and restore

- **Backup...** saves a copy of the document to a file you choose. The name is pre-filled with the date and time, so successive backups do not overwrite each other. A backup does not change the open document, and it is written as it is, without validation: if the document has errors they are kept, which makes a backup useful to recover from a bad edit.
- **Restore from Backup...** loads a FLEF file (plain or compressed, as with Open) and *replaces the model in memory* with it. You are asked to confirm, and then, if the open document has unsaved changes, whether to save them first. This cannot be undone, and the undo history is cleared. The restored data **counts as unsaved** and the document **loses the name of its file**: the next save asks where to write, so it cannot overwrite the original file with the restored data by mistake.

## Checking a file

**Tools > Validate File...** runs the full FLEF validation on the document and lists the problems, each one linked to the record it concerns. See [Tools](tools.md#validate-file).

## Privacy

Records can carry a privacy setting, and **View > Privacy** decides what the views show. See [Navigating the tree](navigating.md#privacy).
