# Preferences

Your preferences are in the **Appearance** and **Settings** menus and in some entries of the **View** menu. They are kept between sessions in a plain text file, `preferences.properties`, in the folder `.familylegacy` of your home directory. **Settings > Open preferences folder...** opens it.

None of these preferences is stored in the genealogy: they change how the data is *shown*, never the data.

## Language

**Appearance > Language** lists the languages the application has been translated into. Choosing one is saved at once, and the application restarts, because some labels are only set when a window is created. Before it closes, what the document has still to write is written; when it starts again it **opens the file that was open**. If that cannot be done (the file cannot be written and you do not want to go on) the language is left as it was.

## Font

**Appearance > Font...** opens a dialog to choose the font of the whole interface.

- The list contains only the installed fonts that can draw every character used by the interface in the current language, so no label ends up with missing glyphs.
- Type in the search box to narrow the list of installed fonts (word by word, with the *whole words* and *fuzzy* options as in the record selection); each font is drawn in its own face.
- Set the **size** next to the filter.
- The preview at the bottom shows a sample sentence in the chosen font and size.
- **Select** applies and saves the choice; `Esc` closes the dialog and leaves things as they are; **Default** goes back to the font of the look-and-feel.

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

## Interface level

**Settings > Interface Level** chooses how much of FLEF the menus offer. It changes only what is *shown*: the file is the same at every level, nothing is removed, and the choice is kept in your preferences, never in the file.

| Level | For | The Research menu and the reports show |
|---|---|---|
| **Basic** (the default) | Building a family tree, as in a traditional genealogy program. | Nothing: no Research menu. People, groups, events, places, sources and media are all there. |
| **Standard** | Active research. | The Research Board, Research Questions, Activities and Tasks, Identity Hypotheses, the Research Log, and the research question and research progress reports. |
| **Advanced** | Evidence-based research. | Everything: also Conclusions, the Context Board (historic events, cultural norms, context impacts), Evidence Qualifiers and the Proof Summary. |

The **Research** tab of the record editors (individual, group, event, place, source, media) is shown from the *Standard* level, and the **Context** tab from the *Advanced* level; the editors of the research and context records themselves always show all their tabs.

**Provenance** (in the Source menu and the right-click menu of an individual), **Citation Analysis** (Source menu) and **Manage Event Types** (Event menu) are also from the *Standard* level.

Whatever the menus and tabs do not show is still one search away: open the [Research Assistant](assistant.md) (Ctrl+Shift+A) and type its name, for example *Conclusions*; it lists the tools of every level.

### Suggestions in the editors

When the Research Assistant finds something an editor can help with, a quiet line appears at the bottom of the editor of the record (it never opens a window, never takes the focus, and the **x** hides it). It is worded for your level:

| Situation | Basic | Standard | Advanced |
|---|---|---|---|
| Different sources disagree | *Different sources give different facts here* - Compare the Evidence... | *Conflicting assertions detected* - Create Conclusion... | The same, explaining that a conclusion documents the preferred interpretation and its evidence |
| Possible duplicate | *Possible duplicate person detected* - Compare the Two People... (it creates nothing) | *Possible duplicate individuals detected* - Create Identity Hypothesis... | The same, explaining that nothing is merged |
| Date estimated by a custom with no norm cited | *This date was estimated from a custom of the time* - Document the Custom... | Create Cultural Norm... | The same, with the full terminology |
| A historic event may have influenced an event | *Events of the time may have affected this* - See What Happened... | Create Context Impact... | The same, with the full terminology |
| Something is still unknown | *Something may still be missing here* - Look Into It... | Create Research Question... | The same, with the full terminology |

The record created is the same at every level, and a suggestion only opens the usual editor on a draft: nothing is saved unless you save it. A basic user sees one suggestion at a time, the others two. To avoid constant interruptions, the editor suggests only what a decision can settle: real conflicts between sources (not data errors), strong duplicate candidates, the highest-value research gaps (not "no source" or "probably dead", which almost every person has), and whether to create a question or a task is said in the link. The rest is in the Research Assistant. The same findings are in the [Research Assistant](assistant.md), where you can ignore them. The **x** hides the line and ignores what it said: the assistant lists it with the ignored findings, where it can be shown again. **Settings > Suggestions in the Editors** turns the line off altogether.

The assistant window and its counter in the toolbar follow the level too: a basic user is not counted the findings about the research workflow (questions, activities, tasks, planner) or about the historical context; they appear from the standard and advanced levels. Several deaths in a place in a short time are suggested in the place editor as a possible historic event, and a marriage or a parenthood at an unusual age as something a cultural norm of the time may explain. A place where events happened and whose jurisdiction changed over time (it was part of different areas, it succeeds or was split from other places, or it has a historic name besides the current one) is suggested in the place editor, with a link to its timeline of jurisdictions. **Settings > Identity Assistant...** is shown from the *Standard* level.

## Preferences in the View menu

- **View > Privacy**: what the views show, according to the privacy of each record. See [Navigating the tree](navigating.md#privacy).
- **View > Order of Children and Partners**: how children and partners are sorted. See [Navigating the tree](navigating.md#order-of-children-and-partners).

## Identity Assistant

**Settings > Identity Assistant...** sets what the [Identity Assistant](assistant.md) proposes. It changes only which possible duplicates are shown, never the data; the values are conventions that are not calibrated on real data, which is why you can change them. The values are saved when you close the dialog (with OK or Esc). 

| Setting | Default | Meaning |
|---|---|---|
| Similarity from which a pair of individuals is proposed | 60 | The score of the duplicate search (see [Finding duplicates](merging.md)); lower it to see weaker candidates. |
| Most pairs of individuals proposed | 100 | The ones with the highest score. |
| Similarity of a strong candidate (high confidence) | 90 | From this score a pair is a warning and the confidence proposed for its hypothesis is high. |
| Similarity of medium confidence | 70 | From this score the confidence proposed is medium; below it, low. It cannot be above the previous one. |
| Match probability (%) from which other records are proposed | 70 | For groups, places, sources, repositories, media, cultural norms and historic events. From 50 to 99. |

**Defaults** restores the values of the table. The new values are used the next time the findings are computed.