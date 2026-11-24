# Research

Family Legacy separates three things that are usually mixed:

- **assertions**: what a source says;
- **hypotheses**: possible interpretations, such as "these two records may be the same person";
- **conclusions**: what the researcher decides after weighing the evidence.

Because they are separate records, a wrong conclusion can be revised without losing the evidence it was drawn from. The **Research** menu holds the commands; it is shown from the *Standard* level (and some of its commands only at *Advanced*), see [Interface level](preferences.md#interface-level). The [Research Assistant](assistant.md) reaches all of them at every level.

## Planning the research

- **Research Board...** shows the whole workflow as a tree: *question, activity, task*, with the conclusions of each question next to its activities. Select a node to see its details on the right; double-click it (or press *Edit*) to open its editor. With a question selected, **New** offers *Activity*, *Task* and *Conclusion of the selected question* with the question already filled in. The filter field keeps only the matching branches. **Delete** removes the selected record after asking; deleting a question leaves its activities and tasks in the file, under *(no question)*.
- **Research Questions...**, **Research Activities...**, **Research Tasks...**, **Conclusions...** and **Identity Hypotheses...** open the usual search-and-manage window (the one of the other record types): type to search, double-click to edit, and create or delete from the same window. Each line carries what you need to recognise the record: a question shows how many activities, tasks and conclusions refer to it; an activity and a task show their question (a task also its due date); a conclusion shows how many records it resolves and its narrative. In the task, the **due date** can be typed or picked with the calendar button at the right of the field.

- **Task Dashboard...** is the place to follow the research tasks of the whole file: the **inbox** of the tasks the Research Assistant generated, what is still to be done, what is over, and how far the work is. It is described in [The task dashboard](#the-task-dashboard).

The lists are the way to find and edit the records of one type; the board is the way to see how they fit together.

## The tasks panel

**View > Show Tasks Panel** puts a panel below the views with the research tasks still to be done in the whole file: those whose status is *open* or *in progress* (or that have no status). The panel stays open while you work, and the choice is remembered. Like the Research menu, it is offered from the *Standard* level.

- The tasks are ordered by **due date**, the earliest first (the ones without a due date last), then by **priority** (*high*, then *normal* or none, then *low*). A task whose due date has passed is written in red, and the title line says how many there are (*3 tasks, 1 overdue*).
- Each line shows the due date, the priority, the status, the task, its research question and what it is about (*About*: the records the task names itself, *Related* in its editor, and the ones its research questions are about).
- **Double-click** a task (or press `Enter`) to open its editor.
- **New Task...** creates a task. If the person or group selected in the view is the target of a research question still under way (*open* or *on hold*), the new task already belongs to that question; if there are several, a menu under the button lists them, with *Without a Question* at the end. With nothing selected, or no such question, the task starts with no question.
- **Right-click** a task to change its status without opening the editor (*Mark as completed*, *Mark as abandoned*, ...) or to open its question. A change of status is noted in the audit of the task, as a save from the editor is; a completed or abandoned task leaves the list.
- **Dashboard...** opens the [task dashboard](#the-task-dashboard) (inbox, filters, completion of all the tasks); the panel is built again when you close it.
- **Only the selected record** keeps the tasks about the person or group selected in the view (the ones that name it and the ones of the questions about it), and follows the selection.
- **Click a column header** to sort by it (a second click reverses the order, a third goes back to due date then priority); the sort is kept while you work. The due date and the priority sort by date and by urgency, not alphabetically.
- The **dock button** (next to the **×**) moves the panel to the bottom, top, left or right side of the window; drag the divider to resize it. The side is remembered.
- Limits: a due date that is not written as `YYYY-MM-DD` is shown empty, as if there were none.
- The **×** at the right end hides the panel.

## The task dashboard

**Research > Task Dashboard...** lists every research task of the file, whatever its status, and shows how far the work is. It opens on the **inbox** when the Research Assistant generated tasks that you have not looked at yet, otherwise on what is *to do*.

- The title line counts the tasks by status (*open*, *in progress*, *completed*, *abandoned*), the **overdue** ones (red: still to be done, due date passed) and the ones in the inbox; the bar is the share of the tasks that are **completed**, among the ones that were not given up.
- **View** chooses what to list: the *inbox* (tasks generated by the assistant and still *open*), *to do* (*open* or *in progress*), *done* (*completed* or *abandoned*) or *all*. The tasks still to be done come first, by due date and then by priority.
- The filters narrow the list further: **priority**, **written by** (the Research Assistant, or you), **about** (the kind of record the task is about: individual, group, event, place, source...), **overdue only**, and a text that the description, the rationale or the outcome contains.
- **Click a column header** to sort by it (the priority by urgency, the due date by date, the undated last); the list keeps the usual order until you do.
- **Accept** takes a task of the inbox in charge (it becomes *in progress* and leaves the inbox); **Dismiss** gives it up (*abandoned*). **Mark As** changes the status of any task (completing a task is how the work is tracked); the change is noted in the audit of the task, as a save from the editor is. When a task is marked *completed* or *abandoned* and has no outcome yet, you are asked what came out of it (it is written in the task's outcome; leave it empty to write nothing, cancel to leave the task as it is). A task is never deleted by these buttons, and the finding that generated it is not proposed again.
- **Open Record** goes to one of the records the task is about (it is not limited to its questions); **Edit** (or a double click) opens the task. The details at the bottom show why the task was proposed.

The tasks are generated from the findings of the [Research Assistant](assistant.md#generating-research-tasks): nothing is created until you confirm.

## The context board

**Research > Context Board...** shows the historical and cultural context as a tree, and is where historic events, cultural norms and context impacts are created and edited. There are two sections: **Historic events** (wars, epidemics, ..., in chronological order, those with no date last) and **Cultural norms** (customs, laws and practices of a time and place, by title). Under each one are the **impacts** it had, written as `→` the record it influenced, with the type of impact and the confidence. Each line has the type and the date (for a norm, the period it was valid), the complete place and the number of impacts. Impacts that cite no existing context are under **(no context)**, so nothing is hidden.

- Select a node to read its details on the right: for a context, its data and the list of the records it acts on; for an impact, its type, the rationale and the confidence.
- Double-click a node, or press **Edit**, to open its editor; the tree is rebuilt after a save and keeps the selection.
- **New** offers *Historic Event*, *Cultural Norm*, *Context Impact* and, with a context (or one of its impacts) selected, *Impact of the selected context*, which opens a new impact with the context already filled in.
- **Delete** removes the selected record after asking. Deleting a context does not delete its impacts: they stay in the file, under *(no context)*, until you assign or delete them.
- The **Filter** box keeps only the branches whose text matches; **Expand all** and **Collapse all** open and close the tree.

The context is not proof that something happened to a person: an impact is an interpretation, and says why. The [Research Assistant](assistant.md) can suggest impacts that are worth checking.

## Weighing the evidence

- **Conclusions...** lists every conclusion; **Identity Hypotheses...** lists the hypotheses, each with the two candidates it compares. An identity hypothesis is how you record "these two individuals may be the same person" without merging them. A conclusion on the hypothesis then says whether it is proven or disproven.

A conclusion lists the records it resolves, states the **proof status** and explains the reasoning. The assertions it is about stay as they are.

## Evidence qualifiers

Each citation of a source can carry *evidence qualifiers*, which say how good a piece of evidence is, along three dimensions:

- the **source type**: original or derived (a source can also say which source it is derived from, see [Sources made from other sources](editors-sources.md#sources-made-from-other-sources));
- the **information type**: primary, secondary or undetermined;
- the **evidence type**: direct, indirect or negative.

**Research > Evidence Qualifiers...** is a report on how they are distributed in the file: an overview of the citations with and without qualifiers, one tab for each dimension, and a tab **Without qualifiers** that lists the records whose citations lack them, by name (the search box above the list narrows it, word by word, with the *whole words* and *fuzzy* options as in the record selection; double-click one to open it and assess it). The citations with no qualifiers at all are the most common gap in a file that has not been fully assessed.

## Reporting

- **Proof Summary...** shows what has been concluded and how solidly. One tab counts the conclusions for each proof status (*conflicting evidence*, *supported*, *proven*, *disproven*; conclusions with another status are grouped apart, so none is dropped); the other lists them and can be filtered by status and opened with a double-click or **Open record**; the report is made again after an edit.
- **Inference Inspection...** opens a conclusion up, with no black box: **why** (the research questions that led to it), **how** (the reasoning chain: the assertions evaluated, the evidence weighed for each, the preferred one, the proof status, your narrative), **on which evidence** (every citation of every assertion, with the evidence qualifiers and a weight), the confidence recorded on the preferred assertion, the **alternative interpretations** and the **conflicting evidence** (citations that go against the preferred assertion, or back an alternative). It also checks the reasoning: no question or narrative, a preferred assertion with no supporting citation, contradicted or outweighed by an alternative, *proven* without direct evidence from an original source. The weights are a rule of thumb (one point per supporting citation, plus one each for an original source, primary information and direct evidence; minus one for a contradicting citation or negative evidence): they never replace your judgement. The same window opens from the Proof Summary with **Inspect inference...**.
- **Research Log...** is a chronological view of every research activity, the oldest first (by the creation date in its audit); activities without a date are placed at the end. Double-click an activity to edit it: the log is made again afterwards.
- **Research Timeline...** is the timeline of the work, not of the lives: when a research task was created or completed, a repository was visited (a search or review that cites a source held by it, or has it as target), a source was added, an extract was created, a citation was added, a conclusion was created or updated, and a conflict was resolved (a conclusion that evaluates at least two assertions and takes a position, as the [Assistant](assistant.md) counts it). It is read from the audit of the records, so it is as precise as they are: a citation has no audit of its own and shows only if it was added to an existing record (the date is the one of that update), and a record with no audit goes at the end, with no date. The filters are the **individual** and the **family** (the entries about them, through the targets of the research questions, the citations and the conclusions), the **source**, the **repository**, and a **date range** (`YYYY-MM-DD`, typed or picked with the calendar button at the right of the field; the undated entries are left out when a date is given). Double-click an entry to edit the record it is about: the timeline is made again afterwards. **Copy all** puts the list in the clipboard.
- **Reports > Research Progress...** produces a report of the state of the research. See [Reports](reports.md).

## Getting help from the assistant

The [Research Assistant](assistant.md) notices open problems and can create drafts of the records that deal with them: a research question for a gap, a conclusion for contradicting evidence, an identity hypothesis for two records that look alike.
