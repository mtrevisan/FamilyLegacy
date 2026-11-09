# Finding duplicates and merging people

Two entries for the same person are common, especially after an [import](import.md). This page explains how to find them, how to compare them, and how to merge them or to record that they are different people.

## Detect Duplicates

**Tools > Detect Duplicates...** looks for individuals that may be the same person. It needs at least two individuals, and it starts searching as soon as it opens (the search runs in the background; a progress bar in the footer, with its percentage, shows how far it is, so that you know when the candidates will appear). Only individuals are examined, and the ones hidden by the current [privacy](privacy.md) policy are left out.

**The controls**

| Control | Meaning |
|---|---|
| **Threshold** | The minimum score for a pair to be shown. The default is 55 (from 0 to 500). Lower it to see weaker candidates. |
| **Max results** | How many candidates to list at most (default 500). |
| **Same sex only** | On by default. With it off, pairs of different sex are compared too. |
| **Skip existing hypotheses** | On by default: pairs already covered by an [identity hypothesis](research.md#weighing-the-evidence), of any kind, are not proposed again. |
| **Search** | Runs the search again with the new settings. |
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
| Birth year | 15 if within 3 years. |
| Death year | 10 if within 3 years. |
| Shared events, attributes, sources, places | 5 for each kind that has something in common. |
| Parents, spouses, children, places, occupations, other attributes and source titles **by name** | Only when the two individuals have no record in common of that kind (a tree that is in the file twice shares none, only records with the same name). Parents: 12 if a parent has the same name, or a spelling variant of it; spouses: 8; children: 8; places of the events: 4; occupations: 4; other attributes of the same type (residence, religion...): 3; titles of the sources: 3 (a title agrees only if it is equal or a spelling variant, never because it is part of another). A name with a single word does not agree with a longer one (*Giovanni* is not *Giovanni Rossi*), a place with more words is another place (*San Martino* is not *San Martino di Lupari*), and an occupation agrees if its words are among the words of the other (*farmer*, *farmer day laborer*). |

**What cannot be a duplicate.** Some pairs are never proposed, whatever their score, because two records of one person cannot be: a parent and a child, two spouses, a person and one of their ancestors (a grandfather and the grandson of the same name are two people), and two children of the same parents with different given names (they are siblings, twins included; a given name that is only a spelling variant, *Catarina* and *Katarina*, or an initial, still counts as the same).

**What takes points away.** What contradicts costs points, so that a pair that looks alike by name and place but is not the same person falls below the threshold. A given name that does not agree (the family name only is shared): -30. Birth years more than 10 years apart (both known): -20, more than 25: -40. Death years more than 10 years apart: -15. The same given name, with the same parents, in children born 2 or more years apart: -25 (the first one died, and the name was given again). The differences that cost points say so in the *Differences* column, with the points.

**Common names count less.** The points of a name that matches are multiplied by a factor from 0.55 to 1.25 that depends on how many individuals of the file have it (in the *Details* pane it is written after the feature, for example `(×0.70)`): two *Giovanni Rossi* in a file with a hundred of them are not as alike as two *Ermenegildo Quaglia*. A file of fewer than 50 individuals says nothing about how common a name is, and the factor is 1. A name that is a single word (a family name written as a name of its own) is not a full name.

The differences that cost nothing (different parents, children or spouses, birth or death years 4 to 10 years apart) are listed in the *Differences* column.

**What you can do here.** **Compare records...** (or a double-click on a row) shows the two records side by side, as text: the names, the notes and the citations of the individual (birth, death, places, occupations, sex and relatives are other records, and are not in it). **Compare facts...** shows what each individual *has*, one under the other: the names, the sex, when and where each was born, baptised, died and buried, the parents, spouses and children (compared by name, not by identifier: two records of one person can have parents that are two records too), the occupations, the residences, the other attributes and the sources that cite them. A fact that two or more individuals say differently is amber (a different date, a different father), one that only some have is grey (a place on one side only); a fact no one has is not shown, and a check box hides the ones that are the same. Select more than one row (Ctrl or Shift and click) to compare the individuals of all of them at once, up to six; with one row it is the two of the pair. Neither dialog changes anything, and neither has a Merge button. To merge, close it and use **Merge Individuals...**.

## Merge Individuals

**Tools > Merge Individuals...** merges two individuals into one. It needs at least two individuals. The selected individual is **not** filled in for you: the dialog opens with both fields empty.

> The merge is written to the file's data after a single confirmation. Make a copy first with **File > Backup...**.

### Choosing the two records

The dialog has two fields:

- **Source (to be deleted)**: the record that is absorbed and then deleted.
- **Target (to be kept)**: the record that survives.

Right-click a field to **Set...**, **Edit...** or **Clear** it. The source and the target must be two different individuals. As soon as both are set, the table of collisions is built.

### The table of collisions

It lists only the fields where the two records **differ**: both have the field, what they say is not the same, and the field is not the audit or the privacy. For each one you see the field, the value in the target, the value in the source and the **Action**:

| Action | Meaning |
|---|---|
| **Keep target** | The target's value stays; the source's is not kept. |
| **Keep source** | The source's value replaces the target's. |
| **Keep both** | The values are joined, without repeating what the target already says. Offered only for fields that can have several values (names, for example). |

The suggestion is **Keep both** for fields that can repeat and **Keep target** for the others. A row you decided yourself turns pale green and shows a check mark; the arrow in the last column (or the `Del` key) goes back to the suggestion. Rows you did not touch still apply the suggestion when you merge.

**Composing a value.** Double-click a row to open the comparison of the field, with three columns: the **source** (deleted) on the left, the **result** (kept) in the middle, the **target** (existing) on the right. Rows that differ are coloured. The result is a text you can edit; for each row, the arrow buttons copy the content of the source or of the target into the result, and the cross removes the row from the result. **Ok** checks that the result is valid FLEF text and that the whole file still validates; if it does not, you stay in the dialog and the errors are listed.

### What the merge does

After you confirm (*Merge [source] individual into [target] individual? The source record will be deleted. All references to it will be re-pointed to the target.*):

1. The fields you chose are applied. A field that can have only one value cannot be left with two: the merge stops with a message.
2. Every **other** field is merged by the automatic rules: repeated fields are joined (a statement that the target already makes is not repeated; its citations and notes are added to it), a single-valued field keeps the target's value or takes the source's if the target has none, the **privacy keeps the most restrictive level**, and the audit stays the target's.
3. The target gets an audit update, *Merged record* followed by the source's identifier. That is the only trace of the source.
4. The source is **deleted**, and every reference to it (relationships, participations, citations, groups...) is rewritten to point to the target.
5. Records that became identical because of the merge are **collapsed** into one, with their citations and notes joined: relationships, attributes, place relationships, events, participations, context impacts and conclusions, in that order. Statements that still differ are kept as competing assertions. Two events collapse only if their principals are the same, so twins keep their own events.
6. Identity hypotheses that now name the same record twice are deleted (this is *accepting* the hypothesis), together with what depends on them.

When it is done, a message says **Merge completed** and lists how many fields were taken from the source, how many references were re-pointed, which duplicates were collapsed, which records were deleted, and, if any, the values of the source that were **not kept**.

There is no preview of the re-pointing and the collapsing: the table only shows the fields that differ.

### Different Records...

If the two records are different people, do not merge. Set both fields and press **Different Records...**. You may type why (a blank answer is accepted). Nothing is merged or deleted; the application records an identity hypothesis for the pair and a conclusion that **disproves** it, and the pair is not proposed again by the duplicate search, unless a more recent conclusion says otherwise.

### Examples

**Two entries for the same man.** Tools > Merge Individuals; set the source (the weaker entry) and the target (the one you want to keep); read the table: *name* rows suggest *Keep both*, so both spellings stay as names; change a row you disagree with; press **Merge** and confirm. Everything that pointed to the source now points to the target.

**A conflict on a field with a single value.** Double-click the row; in the middle column take the source or the target value with the arrow buttons (or type the value you want); press **Ok**. The row turns green.

**They are not the same person.** Set both, press **Different Records...**, write *Different baptism parish*. The pair is recorded as different and will not come up again.
