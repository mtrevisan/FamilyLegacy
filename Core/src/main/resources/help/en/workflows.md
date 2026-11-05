# Worked examples

Each example puts together commands that are explained one by one in the other pages. The links lead to the details.

## 1. Starting from a file made by another program

1. **Make a copy** of what you have open, with **File > Backup...** (the import replaces the open document).
2. **File > Import**, choose GEDCOM or Gramps XML, confirm, answer the question about the open document, and choose where to save the new `.flef`. The new file opens. See [Importing from GEDCOM and Gramps](import.md).
3. **Tools > Validate File...** and fix what it lists, pressing **Re-run** until it says the file is valid. See [When something goes wrong](troubleshooting.md).
4. Look for what the conversion could not express. It leaves **notes** on the records: *Date* (a date that was not understood), *TO BE REVISED* on a spouse relationship that came from a GEDCOM family (decide whether it is civil or religious, and replace the generic type), *Additional file* (a media object with more than one file).
5. Check the **media**: the paths are exactly as in the original file. Open **Source > Manage Media...** and see whether the files exist. If they are all under one folder, set **Appearance > Media paths** to *Relative to the base directory*. See [Preferences](preferences.md#media-paths).
6. Look for **duplicates**: see example 4.
7. Open the [Research Assistant](assistant.md) (`Ctrl+Shift+A`) to see what it flags: dates that cannot be right, missing sex, couples with no children.
8. Check the **privacy** of living people: see example 6.
9. Save a first backup of the clean result.

## 2. Recording a person from a source

You found the baptism of Giovanni Rossi in a parish register, on a scanned page.

1. **Source > New Source...**: give it a title (*Parish of San Martino, baptisms 1840-1860*), and, in *Repositories with Citations*, the archive that keeps it (create it with **Source > New Repository...** if needed). In its *Media* list add the scan. See [Sources and media](editors-sources.md).
2. **Individual > Add Individual...**: add a name with its parts (given: *Giovanni*, family: *Rossi*), the sex, and save. Use the type of name *birth* if it matters.
3. **Event > New Event...**: type *birth* (or *adoption*, *burial*...), the description, the **date** as the register writes it (see [Entering dates](dates.md); put the register's wording in *Original Text*), the place, and in the *Sources* tab the citation of the register.
4. **Link the person to the event.** Open the person (`F2`), tab **Participations**, *Create New...*: the participant is already the person, choose the event, and the role *principal*. See [Events, places and context](editors-events-places.md).
5. In the citation, write the **locator**. Instead of typing it, put the cursor in the citation editor, press `Ctrl+Shift+A` and choose the [Source Citation Assistant](citation-assistant.md): type `libro 2, pag. 143, atto 17`, check the result (`Volume: 2, Page: 143, Entry: 17`), press **Apply to Citation**, and save the citation. Set the **Stance** to *supports*, and, if you can tell, the evidence: *original*, *primary*, *direct*.
6. Add the **parents**: edit the child, tab **Relationships**, add a relationship with the father as target and the type *biological child*, and the same for the mother. See [Relationships](relationships.md#the-parents-of-an-individual).
7. Set a **Confidence** on what is uncertain, and save.

## 3. Two sources disagree

The baptism says 12 March 1841, the military record says 1842.

1. **Do not overwrite** one with the other. Record each statement as it is, each with its own source: two birth events (or two dates of the same event, as you prefer), each cited by its source. Mark the reliability with the evidence qualifiers (*original* and *primary* for the register, *derived* for a copy).
2. The [Research Assistant](assistant.md) reports **conflicting events**: open its finding, which lists the records involved.
3. Open a **research question** with **Research > Research Questions...** (*When was Giovanni born?*), with the person as the *Target*.
4. Record your **activities** (**Research > Research Activities...**): *search* of the baptism index (*Result*: *positive*), *review* of the military register. For a search that found nothing, say how much of the source you covered (*Search Scope*).
5. Write the **conclusion**: from the finding use **Create Conclusion Draft...**, or **Research > Conclusions...**. The *Issue* is `birth_date`, **Resolves** lists the two competing assertions, **Preferred** is the one you judge right, the **Proof Status** is *supported* (or *proven*, if the standard is met) and the **Narrative** explains why. Nothing is deleted: both assertions stay. See [Research records](editors-research.md#conclusion).
6. Use **Research > Research Board...** to see the question, its activities, tasks and conclusions together, and **Research > Proof Summary...** to see how solid your conclusions are.

## 4. The same person appears twice

1. **Tools > Detect Duplicates...** and look at the candidates, from the highest score. Use **Compare records...** on a pair. See [Finding duplicates and merging people](merging.md).
2. If they are the **same person**: **Tools > Merge Individuals...** (make a backup first). Choose as the *target* the entry you want to keep, and read the table of collisions: leave *Keep both* for names, decide the single-valued fields. Merge, and read the summary.
3. If you are **not sure**: do not merge. Record an *identity hypothesis* (**Research > Identity Hypotheses...**) and let a conclusion decide later.
4. If they are **different people** with similar names: in the merge dialog press **Different Records...** and write why. The pair will not be proposed again.

## 5. A line of ancestors for a relative

1. Select the person at the end of the line in the tree. Check what the tree shows with the [views](views.md).
2. **Reports > Ancestor Report...**. Tick *Markdown*, *HTML*, *Word* or *PDF* (see the note on PDF fonts in [Reports](reports.md#the-formats)).
3. Choose the **privacy**: *Hide Restricted and Confidential* leaves out the people who may be alive. Leave *Collaterals* on *none* for the direct line, or choose *siblings* to add uncles and aunts.
4. Choose the **language** and press **Generate...**, choosing a **folder**: the file is written there as `ancestor_report` with the extension of the format. Remember that HTML and Markdown refer to your images where they are, and an existing report with the same name is replaced.

## 6. Before sharing

1. **View > Privacy** > *Hide Restricted and Confidential*, and look at the tree: the people who may be living are masked. See [Privacy](privacy.md).
2. A person who is certainly dead is treated as possibly living if there is no death event and the birth is not older than 110 years: give them a **death event**, even with no date, or an explicit *public* privacy block.
3. Mark with *restricted* or *confidential* the records that must not travel, with a reason.
4. Share a **report** with the same policy rather than the `.flef` file, which always contains everything.

## 7. A routine

At the end of a work session:

1. **Tools > Validate File...** and fix the errors.
2. Look at the **Assistant** indicator in the toolbar (errors, warnings, notes) and at the new findings.
3. **File > Backup...** with the suggested dated name.
4. There is nothing to save: the changes are already in the file. **File > Save as...** is for continuing on another file.

From time to time: **Tools > Statistics...** to see what is missing (individuals with no birth, no parents, no source), **Source > Citation Analysis...** for the unused sources and the uncited assertions, and **Tools > Consanguinity Report...** if the tree is closed in on itself.
