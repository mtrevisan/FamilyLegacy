# Preferences

Your preferences are in the **Settings** menu (the look of the application is in its **Appearance** submenu) and in some entries of the **View** menu. They are kept between sessions in a plain text file, `preferences.properties`, in the folder `.familylegacy` of your home directory. **Settings > Open preferences folder...** opens it.

None of these preferences is stored in the genealogy: they change how the data is *shown*, never the data.

## Language

**Settings > Appearance > Language** lists the languages the application has been translated into. Choosing one is saved at once, and the application restarts, because some labels are only set when a window is created. Before it closes, what the document has still to write is written; when it starts again it **opens the file that was open**. If that cannot be done (the file cannot be written and you do not want to go on) the language is left as it was.

## Font

**Settings > Appearance > Font...** opens a dialog to choose the font of the whole interface.

- The list contains only the installed fonts that can draw every character used by the interface in the current language, so no label ends up with missing glyphs.
- Type in the search box to narrow the list of installed fonts (word by word, with the *whole words* and *fuzzy* options as in the record selection); each font is drawn in its own face.
- Set the **size** next to the filter.
- The preview at the bottom shows a sample sentence in the chosen font and size.
- **Select** applies and saves the choice; `Esc` closes the dialog and leaves things as they are; **Default** goes back to the font of the look-and-feel.

## Media paths

A media record points to a file (see [Sources and media](sources-and-media.md#media)). Full paths are long, and mostly the same at the beginning, so **Settings > Appearance > Media paths** decides how a location is *shown* wherever the application lists media:

- **Full path**: exactly as stored (the default).
- **File name only**: just the name of the file. Short, but two files with the same name in different folders look alike.
- **Relative to the base directory**: the path without the base directory, so `parish\1633\birth.jpg` instead of `C:\Archive\parish\1633\birth.jpg`.

Details:

- The **base directory** is the folder of the open `.flef` file, unless you choose another with **Base directory...**. **Use the folder of the open file** removes your choice and goes back to the default. The tooltip of *Base directory...* tells which folder is in use.
- A file that is not under the base directory, and a web address, are always shown in full.
- Choosing *Relative to the base directory* with no file open and no folder chosen asks for the folder.
- The setting only changes what is *shown*, everywhere a location appears: the lists of media, the *Location* field of the media editor, error messages, the *media missing* warning of the validation, tooltips and the media pages of the reports. The stored value is never changed, and it is the whole value that is saved and used to open the file.
- Windows that are already open show the new setting when they are opened again.

## Interface level

**Settings > Interface Level** chooses how much of FLEF the menus offer. It changes only what is *shown*: the file is the same at every level, nothing is removed, and the choice is kept in your preferences, never in the file.

| Level | For | The Research menu and the reports show |
|---|---|---|
| **Basic** (the default) | Building a family tree, as in a traditional genealogy program. | Nothing: no Research menu. People, groups, events, places, sources and media are all there. |
| **Standard** | Active research. | The Research Board, Research Questions, Activities and Tasks, Identity Hypotheses, the GPS Workflow, the Research Log, the Research Timeline, the tasks panel (**View > Show Tasks Panel**), and the research question and research progress reports. |
| **Advanced** | Evidence-based research. | Everything: also Conclusions, the Context Board (historic events, cultural norms, context impacts), Evidence Qualifiers and the Proof Summary. |

The **Research** tab of the record editors (individual, group, event, place, source, media) is shown from the *Standard* level, and the **Context** tab from the *Advanced* level; the editors of the research and context records themselves always show all their tabs. Inside the Research tab each panel has its own level: research questions, activities, tasks and identity hypotheses are from *Standard*, **Conclusions** (and the conclusion badge in the lists of attributes, relationships and sources) and the **Evidence** qualifiers of a source citation only from *Advanced*.

The **Notes** tab of the editors is always there; from the *Standard* level it has a **Stories** list (the notes of kind *story*) above the list of the other notes, below it the one list has all the notes, the stories too, each with its *Kind*.

The dossier shows the proof-status dot of a conclusion and the evidence badge of a row only from the *Advanced* level.

In the report options, **Historic Context and Research** is offered from the *Standard* level, and **Cultural Norms**, **Historic Events** and **Evidence Qualifiers** from the *Advanced* one.

**Provenance** and **Evidence Trace** (in the Source menu and the right-click menu of an individual), **Citation Analysis**, **Documentation Coverage** and **Conflict Dashboard** (Source menu) and **Manage Event Types** (Event menu) are also from the *Standard* level.

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
- **View > Order of Children, Partners and Dossier**: how children, partners and the events and attributes of the dossier are sorted. See [Navigating the tree](navigating.md#order-of-children-partners-and-dossier).

## Research Assistant

**Settings > Research Assistant...** is how you decide what the [Research Assistant](assistant.md) shows and when. It never changes the data; every choice is kept as soon as you make it, and used the next time the findings are computed.

| Setting | Default | Meaning |
|---|---|---|
| Show the count of the findings in the toolbar | on | The **Assistant** indicator. When it is hidden it computes nothing in the background; the palette still computes its findings when you open it. |
| The indicator blinks when a new error appears | on | A few seconds, once, when the number of errors goes up (not at the first count). |
| The palette opens again after an action when the findings changed | on | After you act on a finding and close its editor. Off: you are left to your work. |
| Assistants that look for findings: **Palette** | all on | An assistant switched off is not asked for its findings, which saves its time on a large file; its tools stay in the palette. |
| Assistants that look for findings: **Indicator** | all on | The indicator can count only some of the assistants (the ones that are slow, for instance) while the palette has them all. An assistant that is off for the palette is off for the indicator too. |
| **Last time** | | Next to each assistant, how long it took the last time it looked for findings: on a large file it tells which one to switch off (the comparison of the individuals and the consanguinity are the slow ones). An assistant is not asked again while the records it reads are the same ones, so after an edit of something it does not read (an event, for the consanguinity) the time is the one of the last real look. |
| Keep counts of what I do with the findings | off | Counts, for each **kind** of finding, how many were listed and how many you acted on, opened, showed in the window, ignored, put aside or showed again, and how high in the list the ones you acted on were. **Nothing about your tree** is counted: no name, date, identifier or text, only the kind of a finding (the same in every language) and a number. The counts are kept in the file `assistant-statistics.txt`, in the folder of the preferences (**Settings > Open preferences folder...**), and **add up over the runs**; they are kept apart for each set of numbers of the assistants, since counts made with other weights say something else. **Statistics...** shows them in a table, **Copy as Text** copies them to give them to whoever asks for them, and **Start Again** forgets them. They are never sent anywhere. |

The other tabs of the same window (**Chronology**, **Gaps**, **Plan**, **Context**) hold the numbers that those assistants use as conventions, each in its range (hover a number to see the range and the default). **Chronology**: the youngest and oldest age of a mother and of a father at a birth, the longest time from the death of the father to a birth, the longest lifespan taken to be possible, the shortest time between the births of children of the same mother and the longest between twins. **Gaps**: the years after the birth from which an individual is probably or certainly dead, the years of a generation, the usefulness from which a gap is high or medium, and the weight (0 to 100) of each kind of gap. **Plan** and **Context**: the same for the steps of the plan and for the impacts of the historic context. They are rules of thumb that are not calibrated on real research, which is why you can change them; they only change what is suggested and in which order, never the data, and **Defaults** in each tab restores its numbers. A limit that would be out of order (a medium level above the high one, a probable death after the certain one) is brought back.

## Identity Assistant

**Settings > Identity Assistant...** sets what the [Identity Assistant](assistant.md) proposes. It changes only which possible duplicates are shown, never the data; the values are conventions that are not calibrated on real data, which is why you can change them. The values are saved when you close the dialog (with OK or Esc). 

| Setting | Default | Meaning |
|---|---|---|
| Similarity from which a pair of individuals is proposed | 60 | The score of the duplicate search (see [Finding duplicates](merging.md)); lower it to see weaker candidates. |
| Most pairs of individuals proposed | 100 | The ones with the highest score. |
| Similarity of a strong candidate (high confidence) | 90 | From this score a pair is a warning and the confidence proposed for its hypothesis is high. |
| Similarity of medium confidence | 70 | From this score the confidence proposed is medium; below it, low. It cannot be above the previous one. |
| Match probability (%) from which other records are proposed | 70 | For groups, sources, repositories, media, cultural norms and historic events. From 50 to 99. |

**Defaults** restores the values of the table. The new values are used the next time the findings are computed.