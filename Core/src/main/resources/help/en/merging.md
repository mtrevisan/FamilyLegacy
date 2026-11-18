# Finding duplicates and merging people

Two entries for the same person are common, especially after an [import](import.md). This page explains how to find them, how to compare them, and how to merge them or to record that they are different people.

## Detect Duplicates

**Tools > Detect Duplicates...** looks for individuals that may be the same person. It needs at least two individuals, and it starts searching as soon as it opens (the search runs in the background; a progress bar in the footer, with its percentage, shows how far it is, so that you know when the candidates will appear). Only individuals are examined, and the ones hidden by the current [privacy](privacy.md) policy are left out.

**The controls**

| Control | Meaning |
|---|---|
| **Threshold** | The minimum score for a pair to be shown. The default is 55 (from 0 to 500). Raising it chooses among the candidates already found, at once, with no new search; lowering it needs the weaker pairs that were not kept, so the window says that it searches again, and the search starts by itself a moment after the last change. |
| **Max results** | How many candidates to list at most (default 500). Lowering it, or raising it when the list was not cut, is also immediate. |
| **Same sex only** | On by default. With it off, pairs of different sex are compared too. |
| **Skip existing hypotheses** | On by default: pairs already covered by an [identity hypothesis](research.md#weighing-the-evidence), of any kind, are not proposed again. |
| **Same sex only**, **Skip existing hypotheses** | A change of these also searches again by itself, since they decide which pairs are compared. |
| **Search** | Searches now, without waiting for the pause (the search already starts by itself after a setting is changed). If the candidates found are enough for the settings, they are shown at once. |
| **Filter** | Narrows the list by name, identifier or matched feature. |

**The table** (sortable) has the columns *Score*, *Left*, *Right*, *Matched* (what made the score) and *Differences*. Below it, the **Details** pane lists the score, the two individuals, the matched features with their points (for example `+40 Name`) and the differences.

**How the score is made.** Points are added for what the two individuals have in common, and only pairs that reach the threshold are shown:

| Feature | Points |
|---|---|
| Name | 40 for a full name that matches exactly; 25 if a given name and a family name are both shared; 10 if only one is. Names are compared without capitals, accents and punctuation. A name written in another *spelling* also counts, by the similarity of the letters (no list of names is kept): *Giovanni* and *Giovani*, *Catarina* and *Katarina* are 20 if both names agree this way (6 if only one does), an initial (*G.* and *Giovanni*) is 15; a name of fewer than 4 letters must be the same, and *Marco* is not *Mario*. A name written in another *language* (*Giovanni*, *Johann*) is a different name here. |
| Parents (biological links) | 20 if the parents are the same; 12 if some are shared. |
| Children | 12 if some are shared. |
| Spouses | 10 if some are shared. |
| Siblings | 8 if some are shared (not if the parents are the same record: they would be counted twice). |
| Different trees | 8 if both individuals have relatives (a parent, a child or a spouse) but in trees that are not connected: the same person written twice is often in two trees imported apart. An individual with no relative at all says nothing, and gets nothing. |
| Birth year | 15 if within 3 years. |
| Death year | 10 if within 3 years. |
| Shared events, attributes, sources, places | 5 for each kind that has something in common. |
| Parents, spouses, children, places, occupations, other attributes and source titles **by name** | Only when the two individuals have no record in common of that kind (a tree that is in the file twice shares none, only records with the same name). Parents: 12 if a parent has the same name, or a spelling variant of it; spouses: 8; children: 8; places of the events: 4; occupations: 4; other attributes of the same type (residence, religion...): 3; titles of the sources: 3 (a title agrees only if it is equal or a spelling variant, never because it is part of another). A name with a single word does not agree with a longer one (*Giovanni* is not *Giovanni Rossi*), a place with more words is another place (*San Martino* is not *San Martino di Lupari*), and an occupation agrees if its words are among the words of the other (*farmer*, *farmer day laborer*). |

**What cannot be a duplicate.** Some pairs are never proposed, whatever their score, because two records of one person cannot be: a parent and a child, two spouses, a person and one of their ancestors (a grandfather and the grandson of the same name are two people), and two children of the same parents with different given names (they are siblings, twins included; a given name that is only a spelling variant, *Catarina* and *Katarina*, or an initial, still counts as the same).

**What takes points away.** What contradicts costs points, so that a pair that looks alike by name and place but is not the same person falls below the threshold. A given name that does not agree (the family name only is shared): -30. Birth years more than 10 years apart (both known): -20, more than 25: -40. Death years more than 10 years apart: -15. The same given name, with the same parents, in children born 2 or more years apart: -25 (the first one died, and the name was given again). Two individuals of the **same tree**, no more than 4 steps apart (a step is a parent, a child or a spouse: an uncle and a nephew, two cousins) and with no parent, child or spouse in common: -15, because they are relatives that were told apart (two records of one person under the same parents are not penalized, that is the evidence of a duplicate). The differences that cost points say so in the *Differences* column, with the points.

**Common names count less.** The points of a name that matches are multiplied by a factor from 0.55 to 1.25 that depends on how many individuals of the file have it (in the *Details* pane it is written after the feature, for example `(×0.70)`): two *Giovanni Rossi* in a file with a hundred of them are not as alike as two *Ermenegildo Quaglia*. A file of fewer than 50 individuals says nothing about how common a name is, and the factor is 1. A name that is a single word (a family name written as a name of its own) is not a full name.

The differences that cost nothing (different parents, children or spouses, birth or death years 4 to 10 years apart) are listed in the *Differences* column.

**What you can do here.** A double-click on a row, like **Merge...**, opens the [review of the merge](#reviewing-the-whole-record) of the pair, which is also the way to compare the two records side by side (nothing is written until you confirm it, and **ESC** closes it). **Compare facts...** shows what each individual *has*, one under the other: the names, the sex, when and where each was born, baptised, died and buried, the parents, spouses and children (compared by name, not by identifier: two records of one person can have parents that are two records too), the occupations, the residences, the other attributes and the sources that cite them. A fact that two or more individuals say differently is amber (a different date, a different father), one that only some have is grey (a place on one side only); a fact no one has is not shown, and a check box hides the ones that are the same. Select more than one row (Ctrl or Shift and click) to compare the individuals of all of them at once, up to six; with one row it is the two of the pair. Comparing the facts changes nothing.

To act on a candidate, select **one** row (the two buttons are disabled with none or several selected) and press:

- **Merge...** opens at once the [review of the whole record](#reviewing-the-whole-record) of the pair, without the window that asks for two records (the pair is already chosen). The one with fewer data is proposed as the source (to be deleted) and the other as the target (to be kept). When the review is confirmed, you are asked once more and the merge is written; **ESC** closes the review and brings you back to the list.
- **Different Records...** asks at once why the two are different people (a blank answer is accepted) and records it, as described under [Different Records...](#different-records) below.

When a pair has been merged or recorded as different, the search is run again, so that the pair and the ones that depended on it are no longer listed. **Tools > Merge Individuals...** stays available, for when you already know the two records and do not need the search.

## Merge Individuals

**Tools > Merge Individuals...** merges two individuals into one. It needs at least two individuals. The selected individual is **not** filled in for you: the dialog opens with both fields empty. (From [Detect Duplicates](#detect-duplicates) this window is skipped: **Merge...** opens the review of the pair at once.)

> Nothing is written until you have reviewed the whole record (see [Reviewing the whole record](#reviewing-the-whole-record)) and confirmed. Make a copy first with **File > Backup...**.

### Choosing the two records

The dialog has two fields:

- **Source (to be deleted)**: the record that is absorbed and then deleted.
- **Target (to be kept)**: the record that survives.

Right-click a field to **Set...**, **Edit...** or **Clear** it. The source and the target must be two different individuals. When both are set, **Review and Merge...** (and **Different Records...**) are enabled. **Different Records...** is here because the pair is chosen by hand; the list of duplicates has its own.

### Reviewing the whole record

**Review and Merge...** does not merge at once: it opens a window, in the manner of a three-way file comparison tool, with the whole record in three columns, the **source** (deleted) on the left, the **result** (kept) in the middle and the **target** (existing) on the right. Every field of the two records is there, not only the ones that differ: the fields only one of them has, and the ones that are the same (hidden while *Only the differences* is ticked). A header says which kind each field is.

**How the columns are aligned.**

- A block (a sub-record), from the line that opens it to the one that closes it, faces **at most one** block of another column, at every depth: it is never interrupted by a second one. Where a column has no line it has a grey empty one, which is not part of the text.
- A sub-record that both have and differs faces its counterpart, and its own sub-records are aligned in the same way, so the lines that differ are on the same row. One that only a side has stands whole, facing grey lines.
- **What a block cites weighs the most.** Two blocks that cite the same record (two citations of one source) face each other whatever else they say. Two blocks that cite different records of a kind, with none in common (two citations of two sources), are two different things and never face each other. Otherwise the counterpart is the one with the same tag that has the most lines in common.
- **The only block of its tag on each side is the other's counterpart**, as the crop of an image is, unless the two cite different records (two citations). A field that holds a single value (the preferred image) is always one against the other, whatever they cite: they are the two versions of it, and the record that differs is marked as a different record.
- **The sub-records are shown in the same order in the three columns, whatever order they had**: the order of the target, and what only the source has right after the sub-record that precedes it in the source. It is only how it is shown: the middle column is what is written, and the order of the file is not kept.
- A second block of the result (both names, for example) is below the first, with grey in the other two columns.

**What the colours say.** The colour is **of the line**, as in a file comparison: only the lines that differ are coloured, and the lines that only open or close a group (`name {` and `}`) never are.

| Colour | Meaning |
|---|---|
| Blue | **From the source**: only the source has this line. |
| Green | **From the target**: only the target has this line. |
| Red | **Conflict**: both have something in its place and it is not the same (two different names, two values of a field that holds one). In the result it stays red until you decide it; then it takes the colour of the side it came from. |
| Strong red | **Different record**: both cite a record (a place, a source, an individual...) and it is not the same one. It is a stronger difference than a different text, because it is another entity. |
| Yellow | **Edited**: a line of the result that is not what was proposed. |
| White | The same in both. |

**Who a reference is.** An id says little: after each line that cites a record of the file (`place P3`) the window writes, in grey, the name of the record as the application shows it. Double-click the line to open the record and edit it; the names are written again when you save it.

**Moving between differences.** The buttons at the top go to the **previous** and **next conflict**, and to the **previous** and **next difference** (a difference is any section that is not white). The keys are **F7** and **F8** for the differences, and **Shift+F7** and **Shift+F8** for the conflicts. The section you are on has a blue frame. The narrow strip beside the scroll bar shows where every difference is in the whole record (click one to go there), and the line at the bottom counts the conflicts, how many are not decided yet, and the differences. The window opens on the first line of the record; the buttons take you to the first conflict.

**Deciding.** The middle column is what the kept record will have, written as FLEF text. **Ctrl+Space** suggests what can be written where the cursor is (the tags the field allows, or the values it accepts); the tags are also offered by themselves as you type their first letters, and the values when the cursor is where one goes. Enter or Tab accepts the suggestion, ESC closes the list. In each section, the buttons on the left of the result act on the **whole section**:

- **→** takes the source's text, **←** (on the right) takes the target's, **⊕** takes both (in a conflict), **✕** empties the section, which removes it from the record;
- **↺** goes back to what the result was when the window opened, and the conflict is to be decided again.

Beside each **block that differs** (a sub-record, or a line whose value differs) there are the same buttons for that block alone: **→** takes it from the source, **←** takes it from the target, **✕** drops it. Only the lines of the block change in the result; the rest stays as it is. A block that a side does not have is removed when you take that side. **All conflicts: source** and **All conflicts: target** (at the top) take every conflict that is not decided yet from one side.

You can also edit the text freely. Taking a side, or editing, decides a conflict (it is no longer red). A conflict you do not decide is merged as suggested, and the count at the bottom tells you how many. A field you leave as it was keeps following the automatic rules of the merge (the citations and notes of a statement that the two records make alike are united, for example); a field you changed is written exactly as it is in the middle column.

**Is it valid?** A moment after you change something, the bar at the bottom says whether the result is valid (the same check that **Merge** makes: the text can be read and the whole file still validates with it), or how many problems it has (a closing brace that is missing, for example); hover over it to read the first ones. The bar keeps saying what it said while it checks again, and changes only when the answer is not the same.

**Merge** reads the middle column and, if it is not valid, stays in the window and lists the errors. **ESC** closes the window and goes back to the previous one, with nothing changed. Only then the confirmation is asked.

### What the merge does

After you confirm (*Merge [source] individual into [target] individual? The source record will be deleted. All references to it will be re-pointed to the target.*):

1. The fields you changed in the review are applied as they are in the middle column. A field that can have only one value cannot be left with two.
2. Every **other** field is merged by the automatic rules: repeated fields are joined (a statement that the target already makes is not repeated; its citations and notes are added to it), a single-valued field keeps the target's value or takes the source's if the target has none, the **privacy keeps the most restrictive level**, and the audit stays the target's.
3. The target gets an audit update, *Merged record* followed by the source's identifier. That is the only trace of the source.
4. The source is **deleted**, and every reference to it (relationships, participations, citations, groups...) is rewritten to point to the target.
5. Records that became identical because of the merge are **collapsed** into one, with their citations and notes joined: relationships, attributes, place relationships, events, participations, context impacts and conclusions, in that order. Statements that still differ are kept as competing assertions. Two events collapse only if their principals are the same, so twins keep their own events.
6. Identity hypotheses that now name the same record twice are deleted (this is *accepting* the hypothesis), together with what depends on them.

When it is done, a message says **Merge completed** and lists how many fields were taken from the source, how many references were re-pointed, which duplicates were collapsed, which records were deleted, and, if any, the values of the source that were **not kept**.

There is no preview of the re-pointing and the collapsing: the review shows the fields of the two records.

### Different Records...

If the two records are different people, do not merge. Set both fields and press **Different Records...**. You may type why (a blank answer is accepted). Nothing is merged or deleted; the application records an identity hypothesis for the pair and a conclusion that **disproves** it, and the pair is not proposed again by the duplicate search, unless a more recent conclusion says otherwise.

### Examples

**Two entries for the same man.** Tools > Merge Individuals; set the source (the weaker entry) and the target (the one you want to keep); press **Review and Merge...**. Go from conflict to conflict with the buttons or **Shift+F8**: for each one take a side with **→** or **←**, or both with **⊕** if both names are right. When the bar at the bottom says the result is valid, press **Merge** and confirm. Everything that pointed to the source now points to the target.

**A conflict on a field with a single value.** Take the source or the target value with the arrow beside the line (or type the value you want in the middle column). The line is no longer red.

**One citation is the same source, the other is not.** Two citations of the same source face each other, and you decide the page; a citation of another source stands apart, with its own buttons: take it, or drop it.

**They are not the same person.** Set both, press **Different Records...**, write *Different baptism parish*. The pair is recorded as different and will not come up again.
