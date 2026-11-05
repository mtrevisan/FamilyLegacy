# Preferences

Your preferences are in the **Appearance** menu and in some entries of the **View** menu. They are kept between sessions in a plain text file, `preferences.properties`, in the folder `.familylegacy` of your home directory. **Appearance > Open preferences file...** opens it.

None of these preferences is stored in the genealogy: they change how the data is *shown*, never the data.

## Language

**Appearance > Language** lists the languages the application has been translated into. Choosing one is saved at once, and the application restarts, because some labels are only set when a window is created. Before it closes, what the document has still to write is written; when it starts again it **opens the file that was open**. If that cannot be done (the file cannot be written and you do not want to go on) the language is left as it was.

## Font

**Appearance > Font...** opens a dialog to choose the font of the whole interface.

- Type in the filter field to narrow the list of installed fonts; each font is drawn in its own face.
- Set the **size** next to the filter.
- The preview at the bottom shows a sample sentence in the chosen font and size.
- **Select** applies and saves the choice; **Cancel** (`Esc`) leaves things as they are; **Default** goes back to the font of the look-and-feel.

## Media paths

A media record points to a file (see [Sources and media](sources-and-media.md#media)). Full paths are long, and mostly the same at the beginning, so **Appearance > Media paths** decides how a location is *shown* wherever the application lists media:

- **Full path**: exactly as stored (the default).
- **File name only**: just the name of the file. Short, but two files with the same name in different folders look alike.
- **Relative to the base directory**: the path without the base directory, so `parish\1633\birth.jpg` instead of `C:\Archive\parish\1633\birth.jpg`.

Details:

- The **base directory** is the folder of the open `.flef` file, unless you choose another with **Base directory...**. **Use the folder of the open file** removes your choice and goes back to the default. The tooltip of *Base directory...* tells which folder is in use.
- A file that is not under the base directory, and a web address, are always shown in full.
- Choosing *Relative to the base directory* with no file open and no folder chosen asks for the folder.
- The setting only changes what is *shown*. The field where you edit the location of a media always shows it whole, and the stored value is never changed.
- Windows that are already open show the new setting when they are opened again.

## Preferences in the View menu

- **View > Privacy**: what the views show, according to the privacy of each record. See [Navigating the tree](navigating.md#privacy).
- **View > Order of Children and Partners**: how children and partners are sorted. See [Navigating the tree](navigating.md#order-of-children-and-partners).

## Identity Assistant

**Appearance > Identity Assistant...** sets what the [Identity Assistant](assistant.md) proposes. It changes only which possible duplicates are shown, never the data; the values are conventions that are not calibrated on real data, which is why you can change them.

| Setting | Default | Meaning |
|---|---|---|
| Similarity from which a pair of individuals is proposed | 60 | The score of the duplicate search (see [Finding duplicates](merging.md)); lower it to see weaker candidates. |
| Most pairs of individuals proposed | 100 | The ones with the highest score. |
| Similarity of a strong candidate (high confidence) | 90 | From this score a pair is a warning and the confidence proposed for its hypothesis is high. |
| Similarity of medium confidence | 70 | From this score the confidence proposed is medium; below it, low. It cannot be above the previous one. |
| Match probability (%) from which other records are proposed | 70 | For groups, places, sources, repositories, media, cultural norms and historic events. From 50 to 99. |

**Defaults** restores the values of the table. The new values are used the next time the findings are computed.