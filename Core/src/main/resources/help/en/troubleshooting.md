# When something goes wrong

## Before anything else

- **Keep copies.** Every change is written to the file by itself, through a temporary file that replaces the real one only when it is complete, so an interrupted write (a full disk, a crash, a locked file) leaves the previous version of the file as it was. Even so, each write replaces the old version for good, so **there is no way back to the file as it was yesterday** except a copy: make a copy with **File > Backup...** before a long session of editing, and before an import, a restore or a merge. Name them with the date: the suggested name already has it.
- **There are no unsaved changes**, so the application never asks whether to save. The only question of this kind is for the document that has **no file yet** (the empty one the application starts with, if you cancelled the choice of its file): *Save* lets you choose a file, *Discard* throws the changes away, *Cancel* stops what you were doing.
- **"Unable to save the changes automatically"** is shown once when a write fails; the application tries again at the next change. Fix the cause (free some space, close the program that locks the file) and make any change, or use **Save as...** to another place.

## Opening and saving

| Message | Meaning and what to do |
|---|---|
| **Unable to open the file** | The file cannot be read: it does not exist, you have no permission, or a compressed file is damaged. The open document stays as it is. |
| **The file is not a valid FLEF document** | The text is not valid FLEF. The reason follows, with the **position** (the number of the character) where the reader stopped. See below. |
| **Unable to save the file** | The disk is full or read-only, the file is locked by another program, or you have no permission. The previous version of the file is left as it was. Use **Save as...** to another place. If you close the application while the file cannot be written you are asked whether to go on anyway, because the latest changes would be lost. |
| **The file already exists. Overwrite it?** | Asked by Save as, Backup and import when the name is taken. |

A file is read as UTF-8 text, or as compressed text (it is recognised by itself). **What the reason after "not a valid FLEF document" means:**

- *Missing closing brace at position N*: a `{` has no matching `}`. The file has been cut, or edited by hand with a mistake.
- *Expected identifier at position N*: something that cannot start a tag, such as a stray `}`.
- *Unterminated multiline string at position N*: a text between triple quotes has no end.
- *Unexpected tag: ... at position N*: a block at the top level that is not FLEF. Typically a GEDCOM or another file renamed `.flef`: use **File > Import** instead.

In every case, open the last backup. A file with correct syntax but wrong content **does** open: the problems then show up in **Tools > Validate File**.

## Importing

*The file is not a valid GEDCOM document* (or *Gramps XML document*): the file is not of that format, or damaged, or its structure or character set is not supported. The reason follows. If accented letters come out wrong after a GEDCOM import, the character set was guessed wrongly: re-save the file in UTF-8 from the program that made it. See [Importing from GEDCOM and Gramps](import.md).

## Backup and restore

- **Backup...** writes the file as it is, without validating it. If the file has errors, the backup has them too, which makes it a good recovery copy.
- **Restore from Backup...** replaces the data in memory with that file, plain or compressed (it asks first, with the file name). The content of **the file of the open document** is replaced with it, because the restored data is saved like every change, so make a backup of the current content first if you may need it. It cannot be undone.

## Tools > Validate File

The report has the columns *Error* and *Record*, a *Details* area, and the buttons **Open record**, **Copy all**, **Re-run** and **Close**. It shows only errors (there are no warnings).

The check is **staged**: first the structure, and only if that is clean the references, and only then the business rules. Fixing a batch can therefore reveal new problems: press **Re-run** until it says *The file is valid. No errors found.*

| Message | What it means |
|---|---|
| *Missing required child 'X' under ...* | A required field is absent from a record. Open the record and fill it. |
| *Invalid enum value 'V' at ... Allowed: [...]* | The value is not one of the allowed ones. |
| *Expected cross-reference at ...* | A field must point to another record (an `@ID@`) and holds something else. |
| *Invalid union choice* / *does not match any allowed alternation type* | A field has a form that is not allowed there. |
| *Constraint violation at ...: ...* | A rule between fields is broken: for example *field 'F' is required because 'C' is 'V'*, *expected exactly one of ...*, or a value that must be among those of another field. |
| *Duplicate record ID 'X'* | Two records have the same identifier. |
| *Unresolved cross-reference 'X': target record does not exist* | An identifier is declared and has no record. It has no record to open. |
| *VALID_FROM must be before VALID_TO* | The validity range of a relationship is reversed (compared as text). |
| *IDENTITY must be present twice* / *IDENTITIES must be different records* | An identity hypothesis lacks its two candidates, or names the same record twice. |

The validation checks the dates against each other when they cannot be right whatever the day they stand for (a death before the birth, a child before the parent, an event after the death, a relationship that ends before it begins), judging each date by all the days it may stand for, so an approximate date, a decade or a century never raises a false alarm. A date is checked in itself in the date editor, see [Entering dates](dates.md#what-the-application-tells-you), and the conventions (ages, the length of a life) are findings of the Chronology Assistant.

## Messages in the editors

All the messages of a record editor are titled **Validation Error**:

- *X is required.* and *At least one X is required.*: fill the field named.
- *X and Y must not be the same entity.*: choose two different records (for example, a relationship needs two different people).
- *X is required when Y is 'Z'.*: a field depends on another.
- *Ambiguous Date* and *Unrecognized Date*: see [Entering dates](dates.md#when-a-word-can-be-read-in-several-ways).
- *Unknown X ID.*: the identifier you typed is not in the file; choose the record from the list.

Other messages you may meet:

- **Record not found** / **Record 'X' is no longer in the model**: the record was deleted after a list was drawn. Refresh the list.
- **No editor registered for record type 'X'**: that kind of record has no editor.
- **Error loading image**: the media file moved, or is not an image. Check its location in the media editor, and [the media path preference](preferences.md#media-paths).
- **Choose a source first** and **Choose a target record first** (Add Citation): the two choices are required.
- **Invalid URL** and **Unable to open the URL**: the web address is wrong, or there is no browser. **Opening web pages is not supported on this platform**: the system cannot open a browser.

**Deleting** always asks *Delete ...?* Deleting an event leaves its participations pointing to nothing, and deleting a research question leaves its activities, tasks and conclusions pointing to nothing. Such dangling references appear later in **Validate File** and in the [Research Assistant](assistant.md). Undo (`Ctrl+Z`) takes a deletion back: see [Undo, redo and recent changes](undo.md).

## Merging

*Choose both a source and a target*, *The source and the target must be different records*, *The FLEF grammar is not available: records cannot be merged* (the grammar file is missing from the installation) and *Unable to plan the merge* (the two records are of incompatible kinds). See [Finding duplicates and merging people](merging.md).

## Reporting a problem

1. Open **Help > Diagnostics...**, which shows: the Java version and vendor, the operating system, the language, the memory, and, for the open file, how many records there are of each type. Press **Copy to Clipboard**.
2. Add the version of the application from **Help > About Family Legacy**, what you did, and, if relevant, the report of **Validate File**.
3. Open **Help > Report a Bug...** (it opens the issue page in the browser) and paste.

The diagnostics do not contain your data, the file path or the contents of the log.

**Logs.** By default the application writes its log only to the console of the program that started it, so you see it only if you start the application from a terminal. A rolling log file is prepared in the configuration but switched off.

## The files the application writes

In the folder `.familylegacy` of your home directory:

| File | What it is for |
|---|---|
| `preferences.properties` | Language, font, privacy policy, order of children and partners, how media paths are shown, and the findings you ignored in the Research Assistant for each file. **Deleting it restores every default.** *Appearance > Open preferences folder...* opens its folder (if the system cannot open it from here, a message gives its path). |
| `bookmarks.properties` | The [bookmarks](searching.md#bookmarks); a text file you can edit by hand. |
| `geocoding.properties` | The coordinates already found for places, for the [Chronomap](chronomap.md). |
| `historical-maps.properties` | The catalogue of historical maps found for each area, valid for 30 days. |
| `tiles/` | The map tiles already downloaded, so that maps you have seen work without a connection. |

The **list of recent files** is kept by the system (the registry on Windows), not in this folder. Your genealogical data lives only in the `.flef` files you save.
