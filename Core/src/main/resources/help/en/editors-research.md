# Research records

For what all the editors share (tabs, confidence, privacy, audit) see [The record editors](editors.md). For the commands of the Research menu see [Research](research.md).

## Research question

State what you want to find out, for example *Who were the parents of Giovanni?* Activities and tasks are linked to the question, and a conclusion can say how it was answered. Open it with **Research > Research Questions...** or from the **Research Board** (*New... > Research question*). **Tabs:** Properties, Research, Privacy, Audit.

| Field | Meaning |
|---|---|
| **Title** | A short title. |
| **Question** | The question, in full. |
| **Target** | The records the question is about: an individual, group, event, participation, relationship, attribute, place, place relationship, repository, source, media, conclusion, identity hypothesis, cultural norm or historic event. It is not required. |
| **Status** | *open* (the default), *on hold*, *resolved* or *closed* (shown as abandoned in the status icon). |
| **Conclusion** and its **Confidence** | What you concluded about the question, and how sure you are. |
| **Rationale** | Why you reached the conclusion, or why the question is still open. |

On OK, the title and the question are required. The **Research** tab lists the activities, tasks and conclusions of the question.

## Research activity

One piece of research work, done or planned. It records the work, and what it found (even nothing), not a genealogical fact. Open it with **Research > Research Activities...** or from the board. **Tabs:** Properties, Research, Findings, References, Sources, Privacy, Audit.

**Properties**

| Field | Meaning |
|---|---|
| **Research Questions** | The question the activity belongs to. |
| **Activity Type** | *search* (a repository, index, catalogue or collection), *review* (a specific source or record), *analysis* (of the evidence, correlating pieces of information), *correspondence* (with archives, institutions or people), *interview*, *hypothesis* (formulating or refining one). |
| **Status** | *planned* (not started: it has no result), *in progress*, *completed* (record the result, the observations and the sources consulted), *abandoned* (dropped before completion: say why in the observation). |
| **Action** | What was done or is planned, for example *Searched the baptism index of St. Mary's parish, 1820-1830*. |

**Research**

- **Target**: the person, family, event, place or record being investigated.
- **Search Scope**: how much of the source you looked at, which matters for negative results. The **Type** is *entire source*, *index only* (entries missing from the index may still be in the source), *partial source* or *selected entries*, and the **Detail** says which pages, years, volumes or entries were covered.

**Findings**

- **Result**: *positive* (relevant information found), *negative* (the search was completed and found nothing relevant: record how much of the source was covered), *inconclusive*, *conflicting* (it conflicts with other evidence; a conclusion may resolve it), *unavailable* (the source was inaccessible, unreadable, lost).
- **Observation**: what you objectively saw, without interpreting it (transcriptions, counts, the absence of an entry).
- **Conclusion** and its **Confidence**: the interpretation you draw from the observations. Keep it apart from them.

**References**

- **Parent Activity**: the previous activity from which this one originated (the search that found the document now being reviewed).
- **Research Follow Ups**: the tasks to start when this activity is completed.

On OK, the type, the status and the action are required.

## Research task

A to-do item that follows from the research, for example *Check the 1871 census for the family*. Open it with **Research > Research Tasks...** or from the board. **Tabs:** Properties, Privacy, Audit.

| Field | Meaning |
|---|---|
| **Description** | What to do. |
| **Research Questions** | The question the task belongs to. |
| **Created By** | The research activity that gave rise to it. |
| **Status** | *open* (not yet started), *in progress*, *completed* (note the result in the outcome), *abandoned* (say why in the outcome, so it is not repeated by mistake). |
| **Priority** | blank, *low* (can wait), *normal*, *high* (do it first: other work depends on it, or the opportunity may be lost, for example an archive closing). |
| **Due Date** | Free text. |
| **Outcome** | What came out of the task once completed. |

On OK, the description and the status are required.

## Conclusion

Use it when your sources disagree: it records which assertion you judge to be the right one, how well proven it is, and why. It never alters or deletes the records it evaluates. Open it with **Research > Conclusions...** or from the board or the **Research** tab of a record. **Tabs:** Properties, Research, Sources, Privacy, Audit (no Notes tab).

| Field | Meaning |
|---|---|
| **Issue** | The specific question the conclusion settles: an event type, or a topic such as `birth_date`, `marriage_place`, `parentage`, `death_cause`, `relationship_type`. |
| **Proof Status** | How well the issue is established (below). |
| **Narrative** | Your reasoning: the sources you evaluated, how you compared them and why you chose. This is what makes the conclusion auditable. |
| **Resolves** | The competing assertions or hypotheses this conclusion evaluates: at least two when the status is *disproven*. They are not modified: all of them stay in the file. |
| **Preferred** | The assertion you judge most likely correct. It must be one of the resolved records. Leave it empty when the conclusion is purely negative. |

**Proof status**

| Value | Meaning |
|---|---|
| conflicting evidence | The evidence is conflicting and none prevails: the issue stays open. |
| supported | A preponderance of the evidence supports the preferred assertion, but without absolute certainty. |
| proven | Proven according to the Genealogical Proof Standard: a reasonably exhaustive search, complete citations, sound analysis, conflicts resolved, and a soundly reasoned conclusion. |
| disproven | Proven false. Normally at least two assertions are resolved and no preferred one is set. |

A conclusion can resolve an event, a participation, a relationship, an individual, an attribute, a group, an identity hypothesis, a place, a place relationship, a repository, a source, a media, a cultural norm or a historic event.

On OK, the issue and the proof status are required. The rules in the tooltips (*disproven* needs two resolved records, the preferred assertion must be one of them) are **not** checked by the editor: check them yourself.

## Identity hypothesis

Use it when two records could describe the same real-world entity and you want to keep the possibility on file without merging yet. You accept it by merging the records, after which the hypothesis is deleted. Open it with **Research > Identity Hypotheses...** or from the **Research** tab of an individual, group or place. **Tabs:** Properties, Context, Research, Sources, Audit (no Notes or Privacy tab).

| Field | Meaning |
|---|---|
| **Identity 1** and **Identity 2** | The two records that may be the same entity. They must be of the same kind (two individuals, two groups or two places) and different from each other. |
| **Compare Identities...** | A button that opens the two records side by side. |
| **Rationale** | Why you think they may be the same, for example the same parents and parish in both records. |
| **Confidence** | How likely you think it is that they are the same. The mere existence of a hypothesis does not mean they are probably identical. |

On OK, both identities are required and they must not be the same record. The same-kind rule is not checked by the editor.

Once the hypothesis is saved, **Create New** in the list of conclusions of its **Research** tab opens a new conclusion that already resolves this hypothesis, with the issue set to *identity*.
