# The Research Assistant

The Research Assistant reads the whole tree and points out what is wrong, doubtful or missing, and it offers helpers for the jobs that are hard to do by hand. It only suggests: **nothing changes unless you apply a suggestion**, and a draft is always yours to check.

## Opening it

- Press `Ctrl+Shift+A`, or choose **Tools > Research Assistant...**.
- A palette opens with a search field (*Search actions and diagnostics*). Type to filter, choose an entry, and press `Esc` to close.
- The **Assistant** indicator at the right end of the toolbar always shows the state of the tree: the number of errors, warnings and notes, or *no findings*. While the analysis runs it says *checking...*.

The palette has two sections:

- **Actions**: the helpers you can start.
- **Diagnostics**: the findings about your data, grouped by category: *Dates, Identity, Places, Sources, Relationships, Research, Conclusions, Context*.

A finding can be opened (*Open the Records...*, to visit the records involved one after the other) or **ignored** if you know it is not a problem. Ignored findings are remembered for the file, can be shown again, and can be restored.

## What it looks for

- **Chronology**: a death before the birth, an event before the birth or after the death, a child born before its parent or after the death of a parent, a parent too young or too old, overlapping marriages, an implausible lifespan. The limits are conventions: a finding means "check this", not "this is wrong".
- **Conflicts**: an individual with two values for something that can have only one, several events of the same kind with dates that cannot all be right, two biological mothers or fathers, someone who would be their own ancestor.
- **Identity**: two individuals that may be the same person and that no conclusion has evaluated yet.
- **Gaps in the research**: completed activities without a result, people in a census without a recorded residence, and similar holes.
- **Context**: events that a historic event or a cultural norm of your model may have influenced, and clusters of deaths in a short time that nothing explains. They are possibilities, never statements: nothing is linked unless you create the impact.
- **Planning**: the next steps of an open research question (the records that would answer it and are not yet in the tree, not yet cited, and not yet covered by a task or an activity), and the *brick walls* worth working on: individuals whose parents are unknown, ranked by how much of the tree depends on them. They are ordered by an estimated usefulness, a rule of thumb and not a judgement of your research.
- **Consanguinity**: couples whose partners are related.

For many findings the assistant offers an action such as **Create Conclusion Draft...**, a research question, a historic event, or a context impact. The draft lists the records involved and leaves the reasoning to you.

## The Historical Date Assistant

It opens from the palette (**Actions > Dates**). It interprets and converts, and **Apply to Date** puts the chosen suggestion in the date (you are asked before an existing value is replaced). Its tools:

- **Calendar Conversion**: converts a date between the Julian, Gregorian, Venetian, Florentine, Pisan and the other calendars. For the Gregorian reform you can choose Rome and the Catholic countries (1582), France (1582), Great Britain (1752), or the Julian calendar and the computus of the Orthodox churches.
- **Age Estimator**: estimates the date of birth from the age at death, marriage or another event. The age can be given in completed years or as the ordinal of the year of life (*in his 72nd year*).
- **Relative Date Calculator**: a date a number of days or weeks before or after another one.
- **Date Relative to an Event**: the same, measured from an event of the tree.
- **Liturgical Date Calculator**: Easter, Pentecost, Advent and the other movable feasts, feast-day references such as *Michaelmas 1498* or *3 days after Easter 1400*, and the distance from Easter.
- **Sunrise and Sunset**: the sunrise and sunset at a place, and what they mean for an event given in hours from sunset. This one needs the **Ephemeris plugin**; without it the assistant says so.

The results are suggestions to support your research, not an authority: check the assumptions before applying one.

## The Source Citation Assistant

It is offered only while you are inside a citation editor, and it is described in full, with the words it understands and examples, in [The Source Citation Assistant](citation-assistant.md). In short: it turns an archival reference such as *vol. 3, p. 45, n. 12* into the **locator** of a citation. It also shows clues taken from the media of the source and the repositories named in the reference. **Apply to Citation** writes the locator (after asking, if there is one already), and **Copy Locator** puts it on the clipboard. What it did not understand is listed, so nothing is lost silently. The locator is a draft built from your text: check it before applying it.
